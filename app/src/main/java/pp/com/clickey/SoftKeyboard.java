package pp.com.clickey;

import android.content.Context;
import android.content.res.Resources;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Vibrator;
import android.text.method.MetaKeyKeyListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;
import static pp.com.clickey.ClicKeyUtils.isEmailValid;
import static pp.com.clickey.R.xml.invoice;

/**
 * Created by baryariv on 21/02/2017.
 */

public class SoftKeyboard extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {
    static final boolean DEBUG = false;

    /**
     * This boolean indicates the optional example code for performing
     * processing of hard keys in addition to regular text generation
     * from on-screen interaction.  It would be used for input methods that
     * perform language translations (such as converting text entered on
     * a QWERTY keyboard to Chinese), but may not be used for input methods
     * that are primarily intended to be used for on-screen text entry.
     */
    static final boolean PROCESS_HARD_KEYS = true;

    private FrameLayout rootView;
    private LatinKeyboardView mInputView;
    private CandidateView mCandidateView;
    private CompletionInfo[] mCompletions;

    private StringBuilder mComposing = new StringBuilder();
    private boolean mPredictionOn;
    private boolean mCompletionOn;
    private int mLastDisplayWidth;
    private boolean mCapsLock;
    private long mLastShiftTime;
    private long mMetaState;

    private LatinKeyboard mSymbolsKeyboard;
    private LatinKeyboard mSymbolsShiftedKeyboard;
    private LatinKeyboard mQwertyKeyboard;
    private LatinKeyboard mNumbersKeyboard;

    private LatinKeyboard mCurKeyboard;

    private String mWordSeparators;

    private List<Keyboard.Key> keyList;
    int centreX, centreY;
    private boolean isFirstTime = true;
    private PopupWindow popup;
    private TextView custom;

    private Invoicelayout il;
    private int input = 0;

    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mWordSeparators = getResources().getString(R.string.word_separators);
    }

    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override
    public void onInitializeInterface() {
        if (mQwertyKeyboard != null) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            int displayWidth = getMaxWidth();
            if (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
        }
        mQwertyKeyboard = new LatinKeyboard(this, R.xml.qwerty);
        mSymbolsKeyboard = new LatinKeyboard(this, R.xml.symbols);
        mSymbolsShiftedKeyboard = new LatinKeyboard(this, R.xml.symbols_shift);
    }

    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    @Override
    public View onCreateInputView() {
        rootView = (FrameLayout) getLayoutInflater().inflate(R.layout.input, null);
        mInputView = (LatinKeyboardView) rootView.findViewById(R.id.keyboard);
        mInputView.setOnKeyboardActionListener(this);
        mInputView.setKeyboard(mQwertyKeyboard);
        mInputView.setPreviewEnabled(false);

        custom = (TextView) getLayoutInflater().inflate(R.layout.preview, null);
        popup = new PopupWindow(getApplicationContext());
        popup.setWidth((int) ClicKeyUtils.dpToPx(getApplicationContext(), 60));
        popup.setHeight((int) ClicKeyUtils.dpToPx(getApplicationContext(), 100));
        popup.setBackgroundDrawable(getResources().getDrawable(R.drawable.preview));
        popup.setContentView(custom);
        popup.setClippingEnabled(false);

        retrieveKeys();
        // Set the onTouchListener to be able to retrieve a MotionEvent
        mInputView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // For each key in the key list
                for (Keyboard.Key k : keyList) {
                    // If the coordinates from the Motion event are inside of the key
                    if (k.isInside((int) event.getX(), (int) event.getY())) {
                        // k is the key pressed
                        Log.d("Debugging",
                                "Key pressed: X=" + k.x + " - Y=" + k.y);
//                        int centreX, centreY;
                        if (centreX != (k.width / 2) + k.x || centreY != (k.width / 2) + k.y) {
                            centreX = (k.width / 2) + k.x;
                            centreY = (k.width / 2) + k.y;
//                            onRelease(k.codes[0]);
                            if (!isFirstTime) {
                                onPress(k.codes[0]);
                            }
                            isFirstTime = false;
                        }
                        // These values are relative to the Keyboard View
                        Log.d("Debugging",
                                "Centre of the key pressed: X=" + centreX + " - Y=" + centreY);
                        break;
                    }
                }

                // Return false to avoid consuming the touch event
                return false;
            }
        });

        return rootView;
    }

    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    @Override
    public View onCreateCandidatesView() {
        mCandidateView = new CandidateView(this);
        mCandidateView.setService(this);

        il = new Invoicelayout(getApplicationContext());
        il.setListener(new Invoicelayout.InvoiceListener() {
            @Override
            public void changeKeyboardToNumbers() {
                mNumbersKeyboard = new LatinKeyboard(SoftKeyboard.this, invoice);
                mInputView.setKeyboard(mNumbersKeyboard);
                input = 2;
            }

            @Override
            public void changeKeyboardToLatin(boolean showCandidateView) {
                mInputView.setKeyboard(mQwertyKeyboard);
                setCandidatesViewShown(showCandidateView);
                if (showCandidateView)
                    input = 1;
                else
                    input = 0;
            }

            @Override
            public void setTextInInputConnection(String text) {
                InputConnection ic = getCurrentInputConnection();
                ic.commitText(text, 1);
            }

            @Override
            public void showErrorPopup(String text) {
                showError(text);
            }
        });
        il.addView(mCandidateView);
        il.sendToTxt.requestFocus();

        return il;
    }

    @Override
    public void onComputeInsets(InputMethodService.Insets outInsets) {
        super.onComputeInsets(outInsets);
        if (!isFullscreenMode()) {
            outInsets.contentTopInsets = outInsets.visibleTopInsets;
        }
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        input = 0;
        if(il != null)
            il.resetLayout();

        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        mComposing.setLength(0);
        updateCandidates();

        if (!restarting) {
            // Clear shift states.
            mMetaState = 0;
        }

        mPredictionOn = false;
        mCompletionOn = false;
        mCompletions = null;

        // We are now going to initialize our state based on the type of
        // text being edited.
        switch (attribute.inputType & EditorInfo.TYPE_MASK_CLASS) {
            case EditorInfo.TYPE_CLASS_NUMBER:
            case EditorInfo.TYPE_CLASS_DATETIME:
                // Numbers and dates default to the symbols keyboard, with
                // no extra features.
                mCurKeyboard = mSymbolsKeyboard;
                break;

            case EditorInfo.TYPE_CLASS_PHONE:
                // Phones will also default to the symbols keyboard, though
                // often you will want to have a dedicated phone keyboard.
                mCurKeyboard = mSymbolsKeyboard;
                break;

            case EditorInfo.TYPE_CLASS_TEXT:
                // This is general text editing.  We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).
                mCurKeyboard = mQwertyKeyboard;
                mPredictionOn = true;

                // We now look for a few special variations of text that will
                // modify our behavior.
                int variation = attribute.inputType & EditorInfo.TYPE_MASK_VARIATION;
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mPredictionOn = false;
                }

                if (variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_URI
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_FILTER) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    mPredictionOn = false;
                }

                if ((attribute.inputType & EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    // If this is an auto-complete text view, then our predictions
                    // will not be shown and instead we will allow the editor
                    // to supply their own.  We only show the editor's
                    // candidates when in fullscreen mode, otherwise relying
                    // own it displaying its own UI.
                    mPredictionOn = false;
                    mCompletionOn = isFullscreenMode();
                }

                // We also want to look at the current state of the editor
                // to decide whether our alphabetic keyboard should start out
                // shifted.
                updateShiftKeyState(attribute);
                break;

            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                mCurKeyboard = mQwertyKeyboard;
                updateShiftKeyState(attribute);
        }

        // Update the label on the enter key, depending on what the application
        // says it will do.
        mCurKeyboard.setImeOptions(getResources(), attribute.imeOptions);
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override
    public void onFinishInput() {
        super.onFinishInput();

        // Clear current composing text and candidates.
        mComposing.setLength(0);
        updateCandidates();

        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false);

        mCurKeyboard = mQwertyKeyboard;
        if (mInputView != null) {
            mInputView.closing();
        }
    }

    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        // Apply the selected keyboard to the input view.
        mInputView.setKeyboard(mCurKeyboard);
        mInputView.closing();
    }

    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd,
                                  int newSelStart, int newSelEnd,
                                  int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);

        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
        if (mComposing.length() > 0 && (newSelStart != candidatesEnd
                || newSelEnd != candidatesEnd)) {
            mComposing.setLength(0);
            updateCandidates();
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
            }
        }
    }

    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    @Override
    public void onDisplayCompletions(CompletionInfo[] completions) {
        if (mCompletionOn) {
            mCompletions = completions;
            if (completions == null) {
//                setSuggestions(null, false, false);
                return;
            }

            List<String> stringList = new ArrayList<String>();
            for (int i = 0; i < (completions != null ? completions.length : 0); i++) {
                CompletionInfo ci = completions[i];
                if (ci != null) stringList.add(ci.getText().toString());
            }
//            setSuggestions(stringList, true, true);
        }
    }

    /**
     * This translates incoming hard key events in to edit operations on an
     * InputConnection.  It is only needed when using the
     * PROCESS_HARD_KEYS option.
     */
    private boolean translateKeyDown(int keyCode, KeyEvent event) {
        mMetaState = MetaKeyKeyListener.handleKeyDown(mMetaState,
                keyCode, event);
        int c = event.getUnicodeChar(MetaKeyKeyListener.getMetaState(mMetaState));
        mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
        InputConnection ic = getCurrentInputConnection();
        if (c == 0 || ic == null) {
            return false;
        }

        boolean dead = false;

        if ((c & KeyCharacterMap.COMBINING_ACCENT) != 0) {
            dead = true;
            c = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
        }

        if (mComposing.length() > 0) {
            char accent = mComposing.charAt(mComposing.length() - 1);
            int composed = KeyEvent.getDeadChar(accent, c);

            if (composed != 0) {
                c = composed;
                mComposing.setLength(mComposing.length() - 1);
            }
        }

        onKey(c, null);

        return true;
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // The InputMethodService already takes care of the back
                // key for us, to dismiss the input method if it is shown.
                // However, our keyboard could be showing a pop-up window
                // that back should dismiss, so we first allow it to do that.
                if (event.getRepeatCount() == 0 && mInputView != null) {
                    if (mInputView.handleBack()) {
                        return true;
                    }
                }
                break;

            case KeyEvent.KEYCODE_DEL:
                // Special handling of the delete key: if we currently are
                // composing text for the user, we want to modify that instead
                // of let the application to the delete itself.
                if (mComposing.length() > 0) {
                    onKey(Keyboard.KEYCODE_DELETE, null);
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_ENTER:
                // Let the underlying text editor always handle these.
                return false;

            default:
                // For all other keys, if we want to do transformations on
                // text being entered with a hard keyboard, we need to process
                // it and do the appropriate action.
                if (PROCESS_HARD_KEYS) {
                    if (keyCode == KeyEvent.KEYCODE_SPACE
                            && (event.getMetaState() & KeyEvent.META_ALT_ON) != 0) {
                        // A silly example: in our input method, Alt+Space
                        // is a shortcut for 'android' in lower case.
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) {
                            // First, tell the editor that it is no longer in the
                            // shift state, since we are consuming this.
                            ic.clearMetaKeyStates(KeyEvent.META_ALT_ON);
                            keyDownUp(KeyEvent.KEYCODE_A);
                            keyDownUp(KeyEvent.KEYCODE_N);
                            keyDownUp(KeyEvent.KEYCODE_D);
                            keyDownUp(KeyEvent.KEYCODE_R);
                            keyDownUp(KeyEvent.KEYCODE_O);
                            keyDownUp(KeyEvent.KEYCODE_I);
                            keyDownUp(KeyEvent.KEYCODE_D);
                            // And we consume this event.
                            return true;
                        }
                    }
                    if (mPredictionOn && translateKeyDown(keyCode, event)) {
                        return true;
                    }
                }
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // If we want to do transformations on text being entered with a hard
        // keyboard, we need to process the up events to update the meta key
        // state we are tracking.
        if (PROCESS_HARD_KEYS) {
            if (mPredictionOn) {
                mMetaState = MetaKeyKeyListener.handleKeyUp(mMetaState,
                        keyCode, event);
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    /**
     * Helper function to commit any text being composed in to the editor.
     */
    private void commitTyped(InputConnection inputConnection) {
        if (mComposing.length() > 0) {
            inputConnection.commitText(mComposing, mComposing.length());
            mComposing.setLength(0);
            updateCandidates();
        }
    }

    /**
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    private void updateShiftKeyState(EditorInfo attr) {
        if (attr != null
                && mInputView != null && mQwertyKeyboard == mInputView.getKeyboard()) {
            int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null && ei.inputType != EditorInfo.TYPE_NULL) {
                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
            }
            mInputView.setShifted(mCapsLock || caps != 0);
        }
    }

    /**
     * Helper to determine if a given character code is alphabetic.
     */
    private boolean isAlphabet(int code) {
        if (Character.isLetter(code)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }

    /**
     * Helper to send a character to the editor as raw key events.
     */
    private void sendKey(int keyCode) {
        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    getCurrentInputConnection().commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }

    // Implementation of KeyboardViewListener

    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        playClick(primaryCode);

        if (primaryCode == -10) {
            if (input == 0) {
                setCandidatesViewShown(true);
                input = 1;
            } else {
                setCandidatesViewShown(false);
                input = 0;
            }
        } else {
            if (isWordSeparator(primaryCode)) {
                if (input == 1) {
                    char code = (char) primaryCode;
                    il.sendToTxt.setText(il.sendToTxt.getText() + String.valueOf(code));
                } else if (input == 2) {
                    char code = (char) primaryCode;
                    il.amountTxt.setText(il.amountTxt.getText() + String.valueOf(code));
                }

                // Handle separator
                else {
                    if (mComposing.length() > 0) {
                        commitTyped(getCurrentInputConnection());
                    }
                    sendKey(primaryCode);
                    updateShiftKeyState(getCurrentInputEditorInfo());
                }
            } else if (primaryCode == Keyboard.KEYCODE_DELETE) {
                if (input == 1) {
                    if (il.sendToTxt.getText().length() > 0) {
                        int position = il.sendToTxt.getSelectionStart() - 1;
                        String text = il.sendToTxt.getText().subSequence(0, il.sendToTxt.getSelectionStart() - 1).toString() + il.sendToTxt.getText().subSequence(il.sendToTxt.getSelectionStart(), il.sendToTxt.getText().length()).toString();
                        il.sendToTxt.setText(text);
//                        if(il.sendToTxt.getSelectionStart() == il.sendToTxt.getText().length())
//                            il.sendToTxt.setSelection(il.sendToTxt.getText().length());
//                        else
                        il.sendToTxt.setSelection(position);

                        if (ClicKeyUtils.isEmailValid(il.sendToTxt.getText().toString())) {
                            il.sendToTxt.setTextColor(getResources().getColor(R.color.color_main_red));
                            il.emailDoneBtn.setVisibility(GONE);
                        }
                    }
                } else if (input == 2) {
                    if (il.amountTxt.getText().length() == 2)
                        il.amountTxt.setText("$0");
                    else
                        il.amountTxt.setText(il.amountTxt.getText().subSequence(0, il.amountTxt.getText().length() - 1));
                } else {
                    handleBackspace();
                }
            } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
                handleShift(primaryCode);

            } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
                handleClose();
                return;
            } else if (primaryCode == LatinKeyboardView.KEYCODE_OPTIONS) {
                // Show a menu or somethin'
            } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE
                    && mInputView != null) {
                Keyboard current = mInputView.getKeyboard();
                if (current == mSymbolsKeyboard || current == mSymbolsShiftedKeyboard) {
                    current = mQwertyKeyboard;
                } else {
                    current = mSymbolsKeyboard;
                }
                mInputView.setKeyboard(current);
                if (current == mSymbolsKeyboard) {
                    current.setShifted(false);
                }
            } else if (primaryCode == Keyboard.KEYCODE_DONE) {
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
            } else {
                char code = (char) primaryCode;
                if (input == 1) {
                    il.sendToTxt.setText(il.sendToTxt.getText() + String.valueOf(code));
                    il.sendToTxt.setSelection(il.sendToTxt.getText().length());
                } else if (input == 2) {
                    if (il.amountTxt.getText().equals("$0"))
                        il.amountTxt.setText("$" + String.valueOf(code));
                    else
                        il.amountTxt.setText(il.amountTxt.getText() + String.valueOf(code));
                } else {
                    handleCharacter(primaryCode, keyCodes);
//                    if(mInputView.getSpaceBg().equals("pressed"))
//                        mInputView.changeSpaceBg("normal");
                }
            }
        }

    }

    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        if (mComposing.length() > 0) {
            commitTyped(ic);
        }
        ic.commitText(text, 0);
        ic.endBatchEdit();
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    private void updateCandidates() {
        if (!mCompletionOn) {
            if (mComposing.length() > 0) {
                ArrayList<String> list = new ArrayList<String>();
                list.add(mComposing.toString());
//                setSuggestions(list, true, true);
            } else {
//                setSuggestions(null, false, false);
            }
        }
    }

//    public void setSuggestions(List<String> suggestions, boolean completions,
//                               boolean typedWordValid) {
//        if (suggestions != null && suggestions.size() > 0) {
//            setCandidatesViewShown(true);
//        } else if (isExtractViewShown()) {
//            setCandidatesViewShown(true);
//        }
//        if (mCandidateView != null) {
//            mCandidateView.setSuggestions(suggestions, completions, typedWordValid);
//        }
//    }

    private void handleBackspace() {
        final int length = mComposing.length();
        if (length > 1) {
            mComposing.delete(length - 1, length);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            updateCandidates();
        } else if (length > 0) {
            mComposing.setLength(0);
            getCurrentInputConnection().commitText("", 0);
            updateCandidates();
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL);
        }
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    private void handleShift(int primaryCode) {
        if (mInputView == null) {
            return;
        }

        Keyboard currentKeyboard = mInputView.getKeyboard();
        if (mQwertyKeyboard == currentKeyboard) {
            // Alphabet keyboard
            checkToggleCapsLock();
            mInputView.setShifted(mCapsLock || !mInputView.isShifted());
//            changeShiftDrawableColorOnShiftPressed(currentKeyboard, primaryCode, mCapsLock || !mInputView.isShifted());
        } else if (currentKeyboard == mSymbolsKeyboard) {
            mSymbolsKeyboard.setShifted(true);
            mInputView.setKeyboard(mSymbolsShiftedKeyboard);
            mSymbolsShiftedKeyboard.setShifted(true);
//            changeShiftDrawableColorOnShiftPressed(currentKeyboard, primaryCode, true);
        } else if (currentKeyboard == mSymbolsShiftedKeyboard) {
            mSymbolsShiftedKeyboard.setShifted(false);
            mInputView.setKeyboard(mSymbolsKeyboard);
            mSymbolsKeyboard.setShifted(false);
//            changeShiftDrawableColorOnShiftPressed(currentKeyboard, primaryCode, false);
        }
    }

    private void changeShiftDrawableColorOnShiftPressed(Keyboard currentKeyboard, int primaryCode, boolean isSelected) {
        List<Keyboard.Key> keys = currentKeyboard.getKeys();
        for (int i = 0; i < keys.size() - 1; i++) {
            Keyboard.Key currentKey = keys.get(i);

            //If your Key contains more than one code, then you will have to check if the codes array contains the primary code
            if (currentKey.codes[0] == primaryCode) {
                currentKey.label = null;
                if (isSelected)
                    currentKey.icon = getResources().getDrawable(R.drawable.keyboard_shift);
                else
                    currentKey.icon = getResources().getDrawable(R.drawable.keyboard_shift_selected);

                break; // leave the loop once you find your match
            }
        }
    }

    private void handleCharacter(int primaryCode, int[] keyCodes) {
        if (isInputViewShown()) {
            if (mInputView.isShifted()) {
                primaryCode = Character.toUpperCase(primaryCode);
            }
        }
        if (isAlphabet(primaryCode) && mPredictionOn) {
            mComposing.append((char) primaryCode);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            updateShiftKeyState(getCurrentInputEditorInfo());
            updateCandidates();
        } else {
            getCurrentInputConnection().commitText(
                    String.valueOf((char) primaryCode), 1);
        }
    }

    private void handleClose() {
        commitTyped(getCurrentInputConnection());
        requestHideSelf(0);
        mInputView.closing();
    }

    private void checkToggleCapsLock() {
        long now = System.currentTimeMillis();
        if (mLastShiftTime + 100 > now) {
            mCapsLock = !mCapsLock;
            mLastShiftTime = 0;
        } else {
            mLastShiftTime = now;
        }
    }

    private String getWordSeparators() {
        return mWordSeparators;
    }

    public boolean isWordSeparator(int code) {
        String separators = getWordSeparators();
        return separators.contains(String.valueOf((char) code));
    }

    public void pickDefaultCandidate() {
        pickSuggestionManually(0);
    }

    public void pickSuggestionManually(int index) {
        if (mCompletionOn && mCompletions != null && index >= 0
                && index < mCompletions.length) {
            CompletionInfo ci = mCompletions[index];
            getCurrentInputConnection().commitCompletion(ci);
            if (mCandidateView != null) {
//                mCandidateView.clear();
            }
            updateShiftKeyState(getCurrentInputEditorInfo());
        } else if (mComposing.length() > 0) {
            // If we were generating candidate suggestions for the current
            // text, we would commit one of them here.  But for this sample,
            // we will just commit the current text.
            commitTyped(getCurrentInputConnection());
        }
    }

    public void swipeRight() {
        if (mCompletionOn) {
            pickDefaultCandidate();
        }
    }

    public void swipeLeft() {
        handleBackspace();
    }

    public void swipeDown() {
        handleClose();
    }

    public void swipeUp() {

    }

    public void onPress(int primaryCode) {
        Log.e("keycode", String.valueOf(primaryCode));
//        if (mInputView.getKeyboard() == mNumbersKeyboard || primaryCode == -10 || primaryCode == 10 || primaryCode == 32 || primaryCode == -2 || primaryCode == -5) {
//            if (primaryCode == 32) {
//                mInputView.changeSpaceBg("pressed");
//            }
//        } else {
//            mInputView.setPreviewEnabled(true);
//        }

        if (custom != null)
            custom.setText(String.valueOf((char) primaryCode));
//        popup.setBackgroundDrawable(getResources().getDrawable(R.drawable.p));
//        popup.setContentView(custom);
//        popup.setClippingEnabled(false);
        if (mInputView.getKeyboard() == mNumbersKeyboard || primaryCode == -10 || primaryCode == 10 || primaryCode == 32 || primaryCode == -2 || primaryCode == -5 || primaryCode == -1) {
            if (primaryCode == 32)
                mInputView.changeSpaceBg("pressed");
            popup.dismiss();
        } else {
            if (popup != null && popup.isShowing()) {
                popup.update(centreX - (int) ClicKeyUtils.dpToPx(getApplicationContext(), 30), centreY + (int) ClicKeyUtils.dpToPx(getApplicationContext(), 30), (int) ClicKeyUtils.dpToPx(getApplicationContext(), 60), (int) ClicKeyUtils.dpToPx(getApplicationContext(), 100));
                if (mInputView.getSpaceBg().equals("pressed"))
                    mInputView.changeSpaceBg("normal");
            } else if (popup != null) {
//            popup.setWidth((int) dpToPx(60));
//            popup.setHeight((int) dpToPx(100));
                popup.showAtLocation(mInputView, Gravity.NO_GRAVITY, centreX - (int) ClicKeyUtils.dpToPx(getApplicationContext(), 30), centreY + (int) ClicKeyUtils.dpToPx(getApplicationContext(), 30));
            }
        }
    }

    public void onRelease(final int primaryCode) {
        Log.e("keycode-r", String.valueOf(primaryCode));
//        if (primaryCode == 32) {
//            mInputView.changeSpaceBg("normal");
//        }
//        mInputView.setPreviewEnabled(false);

        if (primaryCode == 32)
            mInputView.changeSpaceBg("normal");
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (popup != null) {
                                        popup.dismiss();
                                    }
                                }
                            }
                , 70);
    }


    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return false;
    }

    private void playClick(int keyCode) {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        Vibrator mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mVibrator.vibrate(1);
        switch (keyCode) {
            case 32:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR);
                break;
            case Keyboard.KEYCODE_DONE:
            case 10:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN);
                break;
            case Keyboard.KEYCODE_DELETE:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE);
                break;
            default:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD);
        }
    }

    public void retrieveKeys() {
        keyList = mInputView.getKeyboard().getKeys();
    }

    public void showError(String text) {
        final RelativeLayout errorPopup = (RelativeLayout) rootView.findViewById(R.id.error_popup);
        Button closeBtn = (Button) errorPopup.findViewById(R.id.close_btn);
        TextView errorTxt = (TextView) errorPopup.findViewById(R.id.error_txt);

        errorPopup.setVisibility(View.VISIBLE);
        errorTxt.setText(text);

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                errorPopup.setVisibility(GONE);
            }
        });
    }
}
