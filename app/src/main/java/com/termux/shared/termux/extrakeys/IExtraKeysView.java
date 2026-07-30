package com.termux.shared.termux.extrakeys;

import android.view.View;
import com.google.android.material.button.MaterialButton;

/**
 * Extracted from the upstream ExtraKeysView.IExtraKeysView nested interface
 * so TerminalExtraKeys's macro logic can be reused without pulling in the
 * full View-rendering class (which depends on com.termux.shared.R theme
 * attributes we don't have — NexShell renders its own extra-keys row in
 * Compose instead).
 */
public interface IExtraKeysView {

    void onExtraKeyButtonClick(View view, ExtraKeyButton buttonInfo, MaterialButton button);

    boolean performExtraKeyButtonHapticFeedback(View view, ExtraKeyButton buttonInfo, MaterialButton button);

}
