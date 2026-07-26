#include <jni.h>
#include <android/log.h>
#include <cerrno>
#include <cstdlib>
#include <cstring>
#include <fcntl.h>
#include <pthread.h>
#include <signal.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <termios.h>
#include <unistd.h>

#define LOG_TAG "NexShellPty"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" {

// Opens a PTY master, forks, execs the given shell with argv/envp in the
// child after making the slave its controlling terminal, and returns the
// master fd + child pid packed into a jlongArray {masterFd, pid}.
JNIEXPORT jlongArray JNICALL
Java_com_nexshell_pty_PtyNative_forkPty(
        JNIEnv *env, jclass /*clazz*/,
        jstring j_cmd, jobjectArray j_args, jobjectArray j_env,
        jstring j_cwd, jint rows, jint cols) {

    int master_fd = posix_openpt(O_RDWR | O_NOCTTY);
    if (master_fd < 0) {
        LOGE("posix_openpt failed: %s", strerror(errno));
        return nullptr;
    }

    if (grantpt(master_fd) != 0) {
        LOGE("grantpt failed: %s", strerror(errno));
        close(master_fd);
        return nullptr;
    }
    if (unlockpt(master_fd) != 0) {
        LOGE("unlockpt failed: %s", strerror(errno));
        close(master_fd);
        return nullptr;
    }

    char slave_name[128];
    if (ptsname_r(master_fd, slave_name, sizeof(slave_name)) != 0) {
        LOGE("ptsname_r failed: %s", strerror(errno));
        close(master_fd);
        return nullptr;
    }

    struct winsize ws{};
    ws.ws_row = static_cast<unsigned short>(rows);
    ws.ws_col = static_cast<unsigned short>(cols);
    ioctl(master_fd, TIOCSWINSZ, &ws);

    const char *cmd = env->GetStringUTFChars(j_cmd, nullptr);
    const char *cwd = j_cwd ? env->GetStringUTFChars(j_cwd, nullptr) : nullptr;

    jsize argc = j_args ? env->GetArrayLength(j_args) : 0;
    char **argv = static_cast<char **>(malloc(sizeof(char *) * (argc + 2)));
    argv[0] = strdup(cmd);
    for (jsize i = 0; i < argc; i++) {
        auto jstr = (jstring) env->GetObjectArrayElement(j_args, i);
        const char *s = env->GetStringUTFChars(jstr, nullptr);
        argv[i + 1] = strdup(s);
        env->ReleaseStringUTFChars(jstr, s);
        env->DeleteLocalRef(jstr);
    }
    argv[argc + 1] = nullptr;

    jsize envc = j_env ? env->GetArrayLength(j_env) : 0;
    char **envp = static_cast<char **>(malloc(sizeof(char *) * (envc + 1)));
    for (jsize i = 0; i < envc; i++) {
        auto jstr = (jstring) env->GetObjectArrayElement(j_env, i);
        const char *s = env->GetStringUTFChars(jstr, nullptr);
        envp[i] = strdup(s);
        env->ReleaseStringUTFChars(jstr, s);
        env->DeleteLocalRef(jstr);
    }
    envp[envc] = nullptr;

    pid_t pid = fork();
    if (pid < 0) {
        LOGE("fork failed: %s", strerror(errno));
        close(master_fd);
        return nullptr;
    }

    if (pid == 0) {
        // Child: become session leader, attach slave as controlling tty.
        setsid();

        int slave_fd = open(slave_name, O_RDWR);
        if (slave_fd < 0) {
            _exit(127);
        }

        ioctl(slave_fd, TIOCSCTTY, 0);

        dup2(slave_fd, STDIN_FILENO);
        dup2(slave_fd, STDOUT_FILENO);
        dup2(slave_fd, STDERR_FILENO);
        if (slave_fd > STDERR_FILENO) close(slave_fd);
        close(master_fd);

        if (cwd) chdir(cwd);

        execve(argv[0], argv, envp);
        // execve only returns on failure
        _exit(127);
    }

    // Parent
    env->ReleaseStringUTFChars(j_cmd, cmd);
    if (cwd) env->ReleaseStringUTFChars(j_cwd, cwd);
    for (jsize i = 0; argv[i]; i++) free(argv[i]);
    free(argv);
    for (jsize i = 0; envp[i]; i++) free(envp[i]);
    free(envp);

    jlongArray result = env->NewLongArray(2);
    jlong values[2] = {master_fd, pid};
    env->SetLongArrayRegion(result, 0, 2, values);
    return result;
}

JNIEXPORT jint JNICALL
Java_com_nexshell_pty_PtyNative_readFd(JNIEnv *env, jclass, jint fd, jbyteArray buf, jint maxLen) {
    jbyte *bytes = env->GetByteArrayElements(buf, nullptr);
    ssize_t n = read(fd, bytes, maxLen);
    env->ReleaseByteArrayElements(buf, bytes, 0);
    return static_cast<jint>(n);
}

JNIEXPORT jint JNICALL
Java_com_nexshell_pty_PtyNative_writeFd(JNIEnv *env, jclass, jint fd, jbyteArray buf, jint len) {
    jbyte *bytes = env->GetByteArrayElements(buf, nullptr);
    ssize_t n = write(fd, bytes, len);
    env->ReleaseByteArrayElements(buf, bytes, JNI_ABORT);
    return static_cast<jint>(n);
}

JNIEXPORT void JNICALL
Java_com_nexshell_pty_PtyNative_resize(JNIEnv *, jclass, jint fd, jint rows, jint cols) {
    struct winsize ws{};
    ws.ws_row = static_cast<unsigned short>(rows);
    ws.ws_col = static_cast<unsigned short>(cols);
    ioctl(fd, TIOCSWINSZ, &ws);
}

JNIEXPORT void JNICALL
Java_com_nexshell_pty_PtyNative_closeFd(JNIEnv *, jclass, jint fd) {
    close(fd);
}

JNIEXPORT jint JNICALL
Java_com_nexshell_pty_PtyNative_waitForExit(JNIEnv *, jclass, jint pid) {
    int status = 0;
    waitpid(pid, &status, 0);
    return WIFEXITED(status) ? WEXITSTATUS(status) : -1;
}

JNIEXPORT void JNICALL
Java_com_nexshell_pty_PtyNative_sendSignal(JNIEnv *, jclass, jint pid, jint sig) {
    kill(pid, sig);
}

} // extern "C"