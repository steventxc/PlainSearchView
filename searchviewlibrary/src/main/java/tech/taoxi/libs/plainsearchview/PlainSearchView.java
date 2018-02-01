package tech.taoxi.libs.plainsearchview;

/**
 * Copyright (C) 2015 Ari C.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuItemImpl;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import tech.taoxi.libs.plainsearchview.util.Util;
import tech.taoxi.libs.plainsearchview.util.adapter.TextWatcherAdapter;
import tech.taoxi.libs.plainsearchview.util.view.MenuView;
import tech.taoxi.libs.plainsearchview.util.view.SearchInputView;
import com.bartoszlipinski.viewpropertyobjectanimator.ViewPropertyObjectAnimator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * A search UI widget that implements a floating search box also called persistent
 * search.
 */
public class PlainSearchView extends FrameLayout {
    private final static String TAG = PlainSearchView.class.getSimpleName();

    private final static long CLEAR_BTN_FADE_ANIM_DURATION = 500;
    private final static int CLEAR_BTN_WIDTH_DP = 48;

    private final static int LEFT_MENU_WIDTH_AND_MARGIN_START_DP = 52;

    private final static float MENU_BUTTON_PROGRESS_ARROW = 1.0f;
    private final static float MENU_BUTTON_PROGRESS_HAMBURGER = 0.0f;

    private final static int MENU_ICON_ANIM_DURATION = 250;

    public final static int LEFT_ACTION_MODE_SHOW_HAMBURGER = 1;
    public final static int LEFT_ACTION_MODE_SHOW_SEARCH = 2;
    public final static int LEFT_ACTION_MODE_SHOW_HOME = 3;
    public final static int LEFT_ACTION_MODE_NO_LEFT_ACTION = 4;
    private final static int LEFT_ACTION_MODE_NOT_SET = -1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({LEFT_ACTION_MODE_SHOW_HAMBURGER, LEFT_ACTION_MODE_SHOW_SEARCH,
            LEFT_ACTION_MODE_SHOW_HOME, LEFT_ACTION_MODE_NO_LEFT_ACTION, LEFT_ACTION_MODE_NOT_SET})
    public @interface LeftActionMode {
    }

    @LeftActionMode
    private final static int ATTRS_SEARCH_BAR_LEFT_ACTION_MODE_DEFAULT = LEFT_ACTION_MODE_NO_LEFT_ACTION;
    private final static boolean ATTRS_DISMISS_ON_KEYBOARD_DISMISS_DEFAULT = false;
    private final static boolean ATTRS_SEARCH_BAR_SHOW_SEARCH_KEY_DEFAULT = true;
    private final static int ATTRS_QUERY_TEXT_SIZE_SP_DEFAULT = 18;
    private final static int ATTRS_SEARCH_BAR_MARGIN_DEFAULT = 0;

    private Activity mHostActivity;

    private View mMainLayout;
    private boolean mIsFocused;
    private OnFocusChangeListener mFocusChangeListener;

    private CardView mQuerySection;
    private OnSearchListener mSearchListener;
    private SearchInputView mSearchInput;
    private int mQueryTextSize;
    private boolean mCloseSearchOnSoftKeyboardDismiss;
    private String mTitleText;
    private boolean mIsTitleSet;
    private int mSearchInputTextColor = -1;
    private int mSearchInputHintColor = -1;
    private View mSearchInputParent;
    private String mOldQuery = "";
    private OnQueryChangeListener mQueryListener;
    private ImageView mLeftAction;
    private OnLeftMenuClickListener mOnMenuClickListener;
    private OnHomeActionClickListener mOnHomeActionClickListener;
    private ProgressBar mSearchProgress;
    private DrawerArrowDrawable mMenuBtnDrawable;
    private Drawable mIconBackArrow;
    private Drawable mIconSearch;
    @LeftActionMode
    int mLeftActionMode = LEFT_ACTION_MODE_NOT_SET;
    private int mLeftActionIconColor;
    private String mSearchHint;
    private boolean mShowSearchKey;
    private boolean mMenuOpen = false;
    private MenuView mMenuView;
    private int mMenuId = -1;
    private int mActionMenuItemColor;
    private int mOverflowIconColor;
    private OnMenuItemClickListener mActionMenuItemListener;
    private ImageView mClearButton;
    private int mClearBtnColor;
    private Drawable mIconClear;
    private boolean mSkipQueryFocusChangeEvent;
    private boolean mSkipTextChangeEvent;
    private View.OnClickListener mLeftMenuClickListener;

    private OnClearSearchActionListener mOnClearSearchActionListener;


    /**
     * Interface for implementing a listener to listen
     * to state changes in the query text.
     */
    public interface OnQueryChangeListener {

        /**
         * Called when the query has changed. It will
         * be invoked when one or more characters in the
         * query was changed.
         *
         * @param oldQuery the previous query
         * @param newQuery the new query
         */
        void onSearchTextChanged(String oldQuery, String newQuery);
    }

    /**
     * Interface for implementing a listener to listen
     * to when the current search has completed.
     */
    public interface OnSearchListener {

        /**
         * Called when the current search has completed
         * as a result of pressing search key in the keyboard.
         * <p/>
         * Note: This will only get called if
         * {@link PlainSearchView#setShowSearchKey(boolean)}} is set to true.
         *
         * @param currentQuery the text that is currently set in the query TextView
         */
        void onSearchAction(String currentQuery);
    }

    /**
     * Interface for implementing a callback to be
     * invoked when the left menu (navigation menu) is
     * clicked.
     * <p/>
     * Note: This is only relevant when leftActionMode is
     * set to {@value #LEFT_ACTION_MODE_SHOW_HAMBURGER}
     */
    public interface OnLeftMenuClickListener {

        /**
         * Called when the menu button was
         * clicked and the menu's state is now opened.
         */
        void onMenuOpened();

        /**
         * Called when the back button was
         * clicked and the menu's state is now closed.
         */
        void onMenuClosed();
    }

    /**
     * Interface for implementing a callback to be
     * invoked when the home action button (the back arrow)
     * is clicked.
     * <p/>
     * Note: This is only relevant when leftActionMode is
     * set to {@value #LEFT_ACTION_MODE_SHOW_HOME}
     */
    public interface OnHomeActionClickListener {

        /**
         * Called when the home button was
         * clicked.
         */
        void onHomeClicked();
    }

    /**
     * Interface for implementing a listener to listen
     * when an item in the action (the item can be presented as an action
     * ,or as a menu item in the overflow menu) menu has been selected.
     */
    public interface OnMenuItemClickListener {

        /**
         * Called when a menu item in has been
         * selected.
         *
         * @param item the selected menu item.
         */
        void onActionMenuItemSelected(MenuItem item);
    }

    /**
     * Interface for implementing a listener to listen
     * to for focus state changes.
     */
    public interface OnFocusChangeListener {

        /**
         * Called when the search bar has gained focus
         * and listeners are now active.
         */
        void onFocus();

        /**
         * Called when the search bar has lost focus
         * and listeners are no more active.
         */
        void onFocusCleared();
    }

    /**
     * Interface for implementing a callback to be
     * invoked when the clear search text action button
     * (the x to the right of the text) is clicked.
     */
    public interface OnClearSearchActionListener {

        /**
         * Called when the clear search text button
         * was clicked.
         */
        void onClearSearchClicked();
    }

    public PlainSearchView(Context context) {
        this(context, null);
    }

    public PlainSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        mHostActivity = Util.getHostActivity(getContext());

        mMainLayout = inflate(getContext(), R.layout.plain_search_layout, this);

        mQuerySection =  findViewById(R.id.search_query_section);
        mClearButton =  findViewById(R.id.clear_btn);
        mSearchInput =  findViewById(R.id.search_bar_text);
        mSearchInputParent = findViewById(R.id.search_input_parent);
        mLeftAction =  findViewById(R.id.left_action);
        mSearchProgress =  findViewById(R.id.search_bar_search_progress);
        initDrawables();
        mClearButton.setImageDrawable(mIconClear);
        mMenuView =  findViewById(R.id.menu_view);

        setupViews(attrs);
    }

    private void initDrawables() {
        mMenuBtnDrawable = new DrawerArrowDrawable(getContext());
        mIconClear = Util.getWrappedDrawable(getContext(), R.drawable.ic_clear_black_24dp);
        mIconBackArrow = Util.getWrappedDrawable(getContext(), R.drawable.ic_arrow_back_black_24dp);
        mIconSearch = Util.getWrappedDrawable(getContext(), R.drawable.ic_search_black_24dp);
    }

    private void setupViews(AttributeSet attrs) {
        if (attrs != null) {
            applyXmlAttributes(attrs);
        }

        setupQueryBar();
    }

    private void applyXmlAttributes(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.PlainSearchView);

        try {
            mQuerySection.getLayoutParams().width = a.getDimensionPixelSize(
                    R.styleable.PlainSearchView_psv_searchBarWidth,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            int searchBarLeftMargin = a.getDimensionPixelSize(
                    R.styleable.PlainSearchView_psv_searchBarMarginLeft,
                    ATTRS_SEARCH_BAR_MARGIN_DEFAULT);
            int searchBarTopMargin = a.getDimensionPixelSize(
                    R.styleable.PlainSearchView_psv_searchBarMarginTop,
                    ATTRS_SEARCH_BAR_MARGIN_DEFAULT);
            int searchBarRightMargin = a.getDimensionPixelSize(
                    R.styleable.PlainSearchView_psv_searchBarMarginRight,
                    ATTRS_SEARCH_BAR_MARGIN_DEFAULT);
            LayoutParams querySectionLP = (LayoutParams) mQuerySection.getLayoutParams();
            querySectionLP.setMargins(searchBarLeftMargin, searchBarTopMargin,
                    searchBarRightMargin, 0);
            mQuerySection.setLayoutParams(querySectionLP);

            setQueryTextSize(a.getDimensionPixelSize(R.styleable.PlainSearchView_psv_searchInputTextSize,
                    ATTRS_QUERY_TEXT_SIZE_SP_DEFAULT));
            setSearchHint(a.getString(R.styleable.PlainSearchView_psv_searchHint));
            setShowSearchKey(a.getBoolean(R.styleable.PlainSearchView_psv_showSearchKey,
                    ATTRS_SEARCH_BAR_SHOW_SEARCH_KEY_DEFAULT));
            setCloseSearchOnKeyboardDismiss(a.getBoolean(R.styleable.PlainSearchView_psv_close_search_on_keyboard_dismiss,
                    ATTRS_DISMISS_ON_KEYBOARD_DISMISS_DEFAULT));

            //noinspection ResourceType
            mLeftActionMode = a.getInt(R.styleable.PlainSearchView_psv_leftActionMode,
                    ATTRS_SEARCH_BAR_LEFT_ACTION_MODE_DEFAULT);
            if (a.hasValue(R.styleable.PlainSearchView_psv_menu)) {
                mMenuId = a.getResourceId(R.styleable.PlainSearchView_psv_menu, -1);
            }
            setBackgroundColor(a.getColor(R.styleable.PlainSearchView_psv_backgroundColor
                    , Util.getColor(getContext(), R.color.background)));
            setLeftActionIconColor(a.getColor(R.styleable.PlainSearchView_psv_leftActionColor
                    , Util.getColor(getContext(), R.color.left_action_icon)));
            setActionMenuOverflowColor(a.getColor(R.styleable.PlainSearchView_psv_actionMenuOverflowColor
                    , Util.getColor(getContext(), R.color.overflow_icon_color)));
            setMenuItemIconColor(a.getColor(R.styleable.PlainSearchView_psv_menuItemIconColor
                    , Util.getColor(getContext(), R.color.menu_icon_color)));
            setClearBtnColor(a.getColor(R.styleable.PlainSearchView_psv_clearBtnColor
                    , Util.getColor(getContext(), R.color.clear_btn_color)));
            int viewTextColor = a.getColor(R.styleable.PlainSearchView_psv_viewTextColor
                    , Util.getColor(getContext(), R.color.dark_gray));
            setViewTextColor(viewTextColor);
            setQueryTextColor(a.getColor(R.styleable.PlainSearchView_psv_viewSearchInputTextColor
                    , viewTextColor));
            setHintTextColor(a.getColor(R.styleable.PlainSearchView_psv_hintTextColor
                    , Util.getColor(getContext(), R.color.hint_color)));

        } finally {
            a.recycle();
        }
    }

    private void setupQueryBar() {

        mSearchInput.setTextColor(mSearchInputTextColor);
        mSearchInput.setHintTextColor(mSearchInputHintColor);

        if (!isInEditMode() && mHostActivity != null) {
            mHostActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        }

        ViewTreeObserver vto = mQuerySection.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Util.removeGlobalLayoutObserver(mQuerySection, this);

                inflateOverflowMenu(mMenuId);
            }
        });

        mMenuView.setMenuCallback(new MenuBuilder.Callback() {
            @Override
            public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {

                if (mActionMenuItemListener != null) {
                    mActionMenuItemListener.onActionMenuItemSelected(item);
                }

                //todo check if we should care about this return or not
                return false;
            }

            @Override
            public void onMenuModeChange(MenuBuilder menu) {
            }

        });

        mMenuView.setOnVisibleWidthChanged(new MenuView.OnVisibleWidthChangedListener() {
            @Override
            public void onItemsMenuVisibleWidthChanged(int newVisibleWidth) {
                handleOnVisibleMenuItemsWidthChanged(newVisibleWidth);
            }
        });

        mMenuView.setActionIconColor(mActionMenuItemColor);
        mMenuView.setOverflowColor(mOverflowIconColor);

        mClearButton.setVisibility(View.INVISIBLE);
        mClearButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearchInput.setText("");
                if (mOnClearSearchActionListener != null) {
                    mOnClearSearchActionListener.onClearSearchClicked();
                }
            }
        });

        mSearchInput.addTextChangedListener(new TextWatcherAdapter() {

            public void onTextChanged(final CharSequence s, int start, int before, int count) {
                //todo investigate why this is called twice when pressing back on the keyboard

                if (mSkipTextChangeEvent || !mIsFocused) {
                    mSkipTextChangeEvent = false;
                } else {
                    if (mSearchInput.getText().toString().length() != 0 &&
                            mClearButton.getVisibility() == View.INVISIBLE) {
                        mClearButton.setAlpha(0.0f);
                        mClearButton.setVisibility(View.VISIBLE);
                        ViewCompat.animate(mClearButton).alpha(1.0f).setDuration(CLEAR_BTN_FADE_ANIM_DURATION).start();
                    } else if (mSearchInput.getText().toString().length() == 0) {
                        mClearButton.setVisibility(View.INVISIBLE);
                    }

                    if (mQueryListener != null && mIsFocused && !mOldQuery.equals(mSearchInput.getText().toString())) {
                        mQueryListener.onSearchTextChanged(mOldQuery, mSearchInput.getText().toString());
                    }

                }

                mOldQuery = mSearchInput.getText().toString();
            }

        });

        mSearchInput.setOnFocusChangeListener(new TextView.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

                if (mSkipQueryFocusChangeEvent) {
                    mSkipQueryFocusChangeEvent = false;
                } else if (hasFocus != mIsFocused) {
                    setSearchFocusedInternal(hasFocus);
                }
            }
        });

        mSearchInput.setOnKeyboardDismissedListener(new SearchInputView.OnKeyboardDismissedListener() {
            @Override
            public void onKeyboardDismissed() {
                if (mCloseSearchOnSoftKeyboardDismiss) {
                    setSearchFocusedInternal(false);
                }
            }
        });

        mSearchInput.setOnSearchKeyListener(new SearchInputView.OnKeyboardSearchKeyClickListener() {
            @Override
            public void onSearchKeyClicked() {
                if (mSearchListener != null) {
                    mSearchListener.onSearchAction(getQuery());
                }
                mSkipTextChangeEvent = true;
                mSkipTextChangeEvent = true;
                if (mIsTitleSet) {
                    setSearchBarTitle(getQuery());
                } else {
                    setSearchText(getQuery());
                }
                setSearchFocusedInternal(false);
            }
        });

        mLeftAction.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isSearchBarFocused()) {
                    setSearchFocusedInternal(false);
                } else {
                    switch (mLeftActionMode) {
                        case LEFT_ACTION_MODE_SHOW_HAMBURGER:
                            if(mLeftMenuClickListener != null){
                                mLeftMenuClickListener.onClick(mLeftAction);
                            }else {
                                toggleLeftMenu();
                            }
                            break;
                        case LEFT_ACTION_MODE_SHOW_SEARCH:
                            setSearchFocusedInternal(true);
                            break;
                        case LEFT_ACTION_MODE_SHOW_HOME:
                            if (mOnHomeActionClickListener != null) {
                                mOnHomeActionClickListener.onHomeClicked();
                            }
                            break;
                        case LEFT_ACTION_MODE_NO_LEFT_ACTION:
                            //do nothing
                            break;
                    }
                }

            }
        });

        refreshLeftIcon();
    }

    //ensures that the end margin of the search input is according to Material specs
    private void handleOnVisibleMenuItemsWidthChanged(int menuItemsWidth) {
        if (menuItemsWidth == 0) {
            mClearButton.setTranslationX(-Util.dpToPx(4));
            int paddingRight = Util.dpToPx(4);
            if (mIsFocused) {
                paddingRight += Util.dpToPx(CLEAR_BTN_WIDTH_DP);
            } else {
                paddingRight += Util.dpToPx(14);
            }
            mSearchInput.setPadding(0, 0, paddingRight, 0);
        } else {
            mClearButton.setTranslationX(-menuItemsWidth);
            int paddingRight = menuItemsWidth;
            if (mIsFocused) {
                paddingRight += Util.dpToPx(CLEAR_BTN_WIDTH_DP);
            }
            mSearchInput.setPadding(0, 0, paddingRight, 0);
        }
    }

    /**
     * Sets the menu button's color.
     *
     * @param color the color to be applied to the
     *              left menu button.
     */
    public void setLeftActionIconColor(int color) {
        mLeftActionIconColor = color;
        mMenuBtnDrawable.setColor(color);
        DrawableCompat.setTint(mIconBackArrow, color);
        DrawableCompat.setTint(mIconSearch, color);
    }

    /**
     * If set, the left menu won't open or close and the client is assumed to handle its
     * clicks.
     *
     * @param onMenuClickListener listener
     */
    public void setOnMenuClickListener(OnLeftMenuClickListener onMenuClickListener) {
        mOnMenuClickListener = onMenuClickListener;
    }

    /**
     * Sets the clear button's color.
     *
     * @param color the color to be applied to the
     *              clear button.
     */
    public void setClearBtnColor(int color) {
        mClearBtnColor = color;
        DrawableCompat.setTint(mIconClear, mClearBtnColor);
    }

    /**
     * Sets the action menu icons' color.
     *
     * @param color the color to be applied to the
     *              action menu items.
     */
    public void setMenuItemIconColor(int color) {
        this.mActionMenuItemColor = color;
        if (mMenuView != null) {
            mMenuView.setActionIconColor(this.mActionMenuItemColor);
        }
    }

    /**
     * Provides clients access to the menu items
     *
     * @return
     */
    public List<MenuItemImpl> getCurrentMenuItems(){
        return mMenuView.getCurrentMenuItems();
    }

    /**
     * Sets the action menu overflow icon's color.
     *
     * @param color the color to be applied to the
     *              overflow icon.
     */
    public void setActionMenuOverflowColor(int color) {
        this.mOverflowIconColor = color;
        if (mMenuView != null) {
            mMenuView.setOverflowColor(this.mOverflowIconColor);
        }
    }

    /**
     * Sets the background color of the search
     * view including the suggestions section.
     *
     * @param color the color to be applied to the search bar and
     *              the suggestion section background.
     */
    public void setBackgroundColor(int color) {
        if (mQuerySection != null) {
            mQuerySection.setCardBackgroundColor(color);
        }
    }

    /**
     * Sets the text color of the search
     * and suggestion text.
     *
     * @param color the color to be applied to the search and suggestion
     *              text.
     */
    public void setViewTextColor(int color) {
        setQueryTextColor(color);
    }

    /**
     * Sets the text color of the search text.
     *
     * @param color
     */
    public void setQueryTextColor(int color) {
        mSearchInputTextColor = color;
        if (mSearchInput != null) {
            mSearchInput.setTextColor(mSearchInputTextColor);
        }
    }

    /**
     * Set the text size of the text in the search box.
     *
     * @param sizePx
     */
    public void setQueryTextSize(int sizePx) {
        mQueryTextSize = sizePx;
        mSearchInput.setTextSize(mQueryTextSize);
    }

    /**
     * Sets the text color of the search
     * hint.
     *
     * @param color the color to be applied to the search hint.
     */
    public void setHintTextColor(int color) {
        mSearchInputHintColor = color;
        if (mSearchInput != null) {
            mSearchInput.setHintTextColor(color);
        }
    }

    /**
     * Set the mode for the left action button.
     *
     * @param mode
     */
    public void setLeftActionMode(@LeftActionMode int mode) {
        mLeftActionMode = mode;
        refreshLeftIcon();
    }

    private void refreshLeftIcon() {
        int leftActionWidthAndMarginLeft = Util.dpToPx(LEFT_MENU_WIDTH_AND_MARGIN_START_DP);
        int queryTranslationX = 0;

        mLeftAction.setVisibility(VISIBLE);
        switch (mLeftActionMode) {
            case LEFT_ACTION_MODE_SHOW_HAMBURGER:
                mLeftAction.setImageDrawable(mMenuBtnDrawable);
                mMenuBtnDrawable.setProgress(MENU_BUTTON_PROGRESS_HAMBURGER);
                break;
            case LEFT_ACTION_MODE_SHOW_SEARCH:
                mLeftAction.setImageDrawable(mIconSearch);
                break;
            case LEFT_ACTION_MODE_SHOW_HOME:
                mLeftAction.setImageDrawable(mMenuBtnDrawable);
                mMenuBtnDrawable.setProgress(MENU_BUTTON_PROGRESS_ARROW);
                break;
            case LEFT_ACTION_MODE_NO_LEFT_ACTION:
                mLeftAction.setVisibility(View.INVISIBLE);
                queryTranslationX = -leftActionWidthAndMarginLeft;
                break;
        }
        mSearchInputParent.setTranslationX(queryTranslationX);
    }

    private void toggleLeftMenu() {
        if (mMenuOpen) {
            closeMenu(true);
        } else {
            openMenu(true);
        }
    }

    /**
     * <p/>
     * Enables clients to directly manipulate
     * the menu icon's progress.
     * <p/>
     * Useful for custom animation/behaviors.
     *
     * @param progress the desired progress of the menu
     *                 icon's rotation: 0.0 == hamburger
     *                 shape, 1.0 == back arrow shape
     */
    public void setMenuIconProgress(float progress) {
        mMenuBtnDrawable.setProgress(progress);
        if (progress == 0) {
            closeMenu(false);
        } else if (progress == 1.0) {
            openMenu(false);
        }
    }

    /**
     * Mimics a menu click that opens the menu. Useful for navigation
     * drawers when they open as a result of dragging.
     */
    public void openMenu(boolean withAnim) {
        mMenuOpen = true;
        openMenuDrawable(mMenuBtnDrawable, withAnim);
        if (mOnMenuClickListener != null) {
            mOnMenuClickListener.onMenuOpened();
        }
    }

    /**
     * Mimics a menu click that closes. Useful when fo navigation
     * drawers when they close as a result of selecting and item.
     *
     * @param withAnim true, will close the menu button with
     *                 the  Material animation
     */
    public void closeMenu(boolean withAnim) {
        mMenuOpen = false;
        closeMenuDrawable(mMenuBtnDrawable, withAnim);
        if (mOnMenuClickListener != null) {
            mOnMenuClickListener.onMenuClosed();
        }
    }

    /**
     * Set the hamburger menu to open or closed without
     * animating hamburger to arrow and without calling listeners.
     *
     * @param isOpen
     */
    public void setLeftMenuOpen(boolean isOpen) {
        mMenuOpen = isOpen;
        mMenuBtnDrawable.setProgress(isOpen ? 1.0f : 0.0f);
    }

    /**
     * Shows a circular progress on top of the
     * menu action button.
     * <p/>
     * Call hidProgress()
     * to change back to normal and make the menu
     * action visible.
     */
    public void showProgress() {
        mLeftAction.setVisibility(View.GONE);
        mSearchProgress.setAlpha(0.0f);
        mSearchProgress.setVisibility(View.VISIBLE);
        ObjectAnimator.ofFloat(mSearchProgress, "alpha", 0.0f, 1.0f).start();
    }

    /**
     * Hides the progress bar after
     * a prior call to showProgress()
     */
    public void hideProgress() {
        mSearchProgress.setVisibility(View.GONE);
        mLeftAction.setAlpha(0.0f);
        mLeftAction.setVisibility(View.VISIBLE);
        ObjectAnimator.ofFloat(mLeftAction, "alpha", 0.0f, 1.0f).start();
    }

    /**
     * Inflates the menu items from
     * an xml resource.
     *
     * @param menuId a menu xml resource reference
     */
    public void inflateOverflowMenu(int menuId) {
        mMenuId = menuId;
        mMenuView.reset(menuId, actionMenuAvailWidth());
        if (mIsFocused) {
            mMenuView.hideIfRoomItems(false);
        }
    }

    private int actionMenuAvailWidth() {
        if (isInEditMode()) {
            return mQuerySection.getMeasuredWidth() / 2;
        }
        return mQuerySection.getWidth() / 2;
    }

    /**
     * Set a hint that will appear in the
     * search input. Default hint is R.string.abc_search_hint
     * which is "search..." (when device language is set to english)
     *
     * @param searchHint
     */
    public void setSearchHint(String searchHint) {
        mSearchHint = searchHint != null ? searchHint : getResources().getString(R.string.abc_search_hint);
        mSearchInput.setHint(mSearchHint);
    }

    /**
     * Sets whether the the button with the search icon
     * will appear in the soft-keyboard or not.
     *
     * @param show to show the search button in
     *             the soft-keyboard.
     */
    public void setShowSearchKey(boolean show) {
        mShowSearchKey = show;
        if (show) {
            mSearchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        } else {
            mSearchInput.setImeOptions(EditorInfo.IME_ACTION_NONE);
        }
    }


    /**
     * Sets whether the search will lose focus when the softkeyboard
     * gets closed from a back press
     *
     * @param closeSearchOnKeyboardDismiss
     */
    public void setCloseSearchOnKeyboardDismiss(boolean closeSearchOnKeyboardDismiss) {
        this.mCloseSearchOnSoftKeyboardDismiss = closeSearchOnKeyboardDismiss;
    }

    /**
     * Wrapper implementation for EditText.setFocusable(boolean focusable)
     *
     * @param focusable true, to make search focus when
     *                  clicked.
     */
    public void setSearchFocusable(boolean focusable) {
        mSearchInput.setFocusable(focusable);
        mSearchInput.setFocusableInTouchMode(focusable);
    }

    /**
     * Sets the title for the search bar.
     * <p/>
     * Note that after the title is set, when
     * the search gains focus, the title will be replaced
     * by the search hint.
     *
     * @param title the title to be shown when search
     *              is not focused
     */
    public void setSearchBarTitle(CharSequence title) {
        this.mTitleText = title.toString();
        mIsTitleSet = true;
        mSearchInput.setText(title);
    }

    /**
     * Sets the search text.
     * <p/>
     * Note that this is the different from
     * {@link #setSearchBarTitle(CharSequence title) setSearchBarTitle} in
     * that it keeps the text when the search gains focus.
     *
     * @param text the text to be set for the search
     *             input.
     */
    public void setSearchText(CharSequence text) {
        mIsTitleSet = false;
        setQueryText(text);
    }

    /**
     * Returns the current query text.
     *
     * @return the current query
     */
    public String getQuery() {
        return mOldQuery;
    }

    public void clearQuery() {
        mSearchInput.setText("");
    }

    /**
     * Sets whether the search is focused or not.
     *
     * @param focused true, to set the search to be active/focused.
     * @return true if the search was focused and will now become not focused. Useful for
     * calling supper.onBackPress() in the hosting activity only if this method returns false
     */
    public boolean setSearchFocused(final boolean focused) {

        boolean updatedToNotFocused = !focused && this.mIsFocused;

        if (focused != this.mIsFocused) {
            setSearchFocusedInternal(focused);
        }
        return updatedToNotFocused;
    }

    private void setQueryText(CharSequence text) {
        mSearchInput.setText(text);
        //move cursor to end of text
        mSearchInput.setSelection(mSearchInput.getText().length());
    }

    public void clearSearchFocus() {
        setSearchFocusedInternal(false);
    }

    public boolean isSearchBarFocused() {
        return mIsFocused;
    }

    private void setSearchFocusedInternal(final boolean focused) {
        this.mIsFocused = focused;

        if (focused) {
            mSearchInput.requestFocus();
            handleOnVisibleMenuItemsWidthChanged(0);//this must be called before  mMenuView.hideIfRoomItems(...)
            mMenuView.hideIfRoomItems(true);
            transitionInLeftSection(true);
            Util.showSoftKeyboard(getContext(), mSearchInput);
            if (mMenuOpen) {
                closeMenu(false);
            }
            if (mIsTitleSet) {
                mSkipTextChangeEvent = true;
                mSearchInput.setText("");
            } else {
                mSearchInput.setSelection(mSearchInput.getText().length());
            }
            mSearchInput.setLongClickable(true);
            mClearButton.setVisibility((mSearchInput.getText().toString().length() == 0) ?
                    View.INVISIBLE : View.VISIBLE);
            if (mFocusChangeListener != null) {
                mFocusChangeListener.onFocus();
            }
        } else {
            mMainLayout.requestFocus();
            handleOnVisibleMenuItemsWidthChanged(0);//this must be called before  mMenuView.hideIfRoomItems(...)
            mMenuView.showIfRoomItems(true);
            transitionOutLeftSection(true);
            mClearButton.setVisibility(View.GONE);
            if (mHostActivity != null) {
                Util.closeSoftKeyboard(mHostActivity);
            }
            if (mIsTitleSet) {
                mSkipTextChangeEvent = true;
                mSearchInput.setText(mTitleText);
            }
            mSearchInput.setLongClickable(false);
            if (mFocusChangeListener != null) {
                mFocusChangeListener.onFocusCleared();
            }
        }
    }

    private void changeIcon(ImageView imageView, Drawable newIcon, boolean withAnim) {
        imageView.setImageDrawable(newIcon);
        if (withAnim) {
            ObjectAnimator fadeInVoiceInputOrClear = ObjectAnimator.ofFloat(imageView, "alpha", 0.0f, 1.0f);
            fadeInVoiceInputOrClear.start();
        } else {
            imageView.setAlpha(1.0f);
        }
    }

    private void transitionInLeftSection(boolean withAnim) {

        if (mSearchProgress.getVisibility() != View.VISIBLE) {
            mLeftAction.setVisibility(View.VISIBLE);
        } else {
            mLeftAction.setVisibility(View.INVISIBLE);
        }

        switch (mLeftActionMode) {
            case LEFT_ACTION_MODE_SHOW_HAMBURGER:
                openMenuDrawable(mMenuBtnDrawable, withAnim);
                if (!mMenuOpen) {
                    break;
                }
                break;
            case LEFT_ACTION_MODE_SHOW_SEARCH:
                mLeftAction.setImageDrawable(mIconBackArrow);
                if (withAnim) {
                    mLeftAction.setRotation(45);
                    mLeftAction.setAlpha(0.0f);
                    ObjectAnimator rotateAnim = ViewPropertyObjectAnimator.animate(mLeftAction).rotation(0).get();
                    ObjectAnimator fadeAnim = ViewPropertyObjectAnimator.animate(mLeftAction).alpha(1.0f).get();
                    AnimatorSet animSet = new AnimatorSet();
                    animSet.setDuration(500);
                    animSet.playTogether(rotateAnim, fadeAnim);
                    animSet.start();
                }
                break;
            case LEFT_ACTION_MODE_SHOW_HOME:
                //do nothing
                break;
            case LEFT_ACTION_MODE_NO_LEFT_ACTION:
                mLeftAction.setImageDrawable(mIconBackArrow);

                if (withAnim) {
                    ObjectAnimator searchInputTransXAnim = ViewPropertyObjectAnimator
                            .animate(mSearchInputParent).translationX(0).get();

                    mLeftAction.setScaleX(0.5f);
                    mLeftAction.setScaleY(0.5f);
                    mLeftAction.setAlpha(0.0f);
                    mLeftAction.setTranslationX(Util.dpToPx(8));
                    ObjectAnimator transXArrowAnim = ViewPropertyObjectAnimator.animate(mLeftAction).translationX(1.0f).get();
                    ObjectAnimator scaleXArrowAnim = ViewPropertyObjectAnimator.animate(mLeftAction).scaleX(1.0f).get();
                    ObjectAnimator scaleYArrowAnim = ViewPropertyObjectAnimator.animate(mLeftAction).scaleY(1.0f).get();
                    ObjectAnimator fadeArrowAnim = ViewPropertyObjectAnimator.animate(mLeftAction).alpha(1.0f).get();
                    transXArrowAnim.setStartDelay(150);
                    scaleXArrowAnim.setStartDelay(150);
                    scaleYArrowAnim.setStartDelay(150);
                    fadeArrowAnim.setStartDelay(150);

                    AnimatorSet animSet = new AnimatorSet();
                    animSet.setDuration(500);
                    animSet.playTogether(searchInputTransXAnim, transXArrowAnim, scaleXArrowAnim, scaleYArrowAnim, fadeArrowAnim);
                    animSet.start();
                } else {
                    mSearchInputParent.setTranslationX(0);
                }
                break;
        }
    }

    private void transitionOutLeftSection(boolean withAnim) {

        switch (mLeftActionMode) {
            case LEFT_ACTION_MODE_SHOW_HAMBURGER:
                closeMenuDrawable(mMenuBtnDrawable, withAnim);
                break;
            case LEFT_ACTION_MODE_SHOW_SEARCH:
                changeIcon(mLeftAction, mIconSearch, withAnim);
                break;
            case LEFT_ACTION_MODE_SHOW_HOME:
                //do nothing
                break;
            case LEFT_ACTION_MODE_NO_LEFT_ACTION:
                mLeftAction.setImageDrawable(mIconBackArrow);

                if (withAnim) {
                    ObjectAnimator searchInputTransXAnim = ViewPropertyObjectAnimator.animate(mSearchInputParent)
                            .translationX(-Util.dpToPx(LEFT_MENU_WIDTH_AND_MARGIN_START_DP)).get();

                    ObjectAnimator scaleXArrowAnim = ViewPropertyObjectAnimator.animate(mLeftAction).scaleX(0.5f).get();
                    ObjectAnimator scaleYArrowAnim = ViewPropertyObjectAnimator.animate(mLeftAction).scaleY(0.5f).get();
                    ObjectAnimator fadeArrowAnim = ViewPropertyObjectAnimator.animate(mLeftAction).alpha(0.5f).get();
                    scaleXArrowAnim.setDuration(300);
                    scaleYArrowAnim.setDuration(300);
                    fadeArrowAnim.setDuration(300);
                    scaleXArrowAnim.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {

                            //restore normal state
                            mLeftAction.setScaleX(1.0f);
                            mLeftAction.setScaleY(1.0f);
                            mLeftAction.setAlpha(1.0f);
                            mLeftAction.setVisibility(View.INVISIBLE);
                        }
                    });

                    AnimatorSet animSet = new AnimatorSet();
                    animSet.setDuration(350);
                    animSet.playTogether(scaleXArrowAnim, scaleYArrowAnim, fadeArrowAnim, searchInputTransXAnim);
                    animSet.start();
                } else {
                    mLeftAction.setVisibility(View.INVISIBLE);
                }
                break;
        }
    }

    /**
     * Sets the listener that will listen for query
     * changes as they are being typed.
     *
     * @param listener listener for query changes
     */
    public void setOnQueryChangeListener(OnQueryChangeListener listener) {
        this.mQueryListener = listener;
    }

    /**
     * Sets the listener that will be called when
     * an action that completes the current search
     * session has occurred and the search lost focus.
     * <p/>
     * <p>When called, a client would ideally grab the
     * search or suggestion query from the callback parameter or
     * from {@link #getQuery() getquery} and perform the necessary
     * query against its data source.</p>
     *
     * @param listener listener for query completion
     */
    public void setOnSearchListener(OnSearchListener listener) {
        this.mSearchListener = listener;
    }

    /**
     * Sets the listener that will be called when the focus
     * of the search has changed.
     *
     * @param listener listener for search focus changes
     */
    public void setOnFocusChangeListener(OnFocusChangeListener listener) {
        this.mFocusChangeListener = listener;
    }

    /**
     * Sets the listener that will be called when the
     * left/start menu (or navigation menu) is clicked.
     * <p/>
     * <p>Note that this is different from the overflow menu
     * that has a separate listener.</p>
     *
     * @param listener
     */
    public void setOnLeftMenuClickListener(OnLeftMenuClickListener listener) {
        this.mOnMenuClickListener = listener;
    }

    /**
     * Sets the listener that will be called when the
     * left/start home action (back arrow) is clicked.
     *
     * @param listener
     */
    public void setOnHomeActionClickListener(OnHomeActionClickListener listener) {
        this.mOnHomeActionClickListener = listener;
    }

    /**
     * Sets the listener that will be called when
     * an item in the overflow menu is clicked.
     *
     * @param listener listener to listen to menu item clicks
     */
    public void setOnMenuItemClickListener(OnMenuItemClickListener listener) {
        this.mActionMenuItemListener = listener;
        //todo reset menu view listener
    }

    /**
     * Sets the listener that will be called when the
     * clear search text action button (the x to the right
     * of the search text) is clicked.
     *
     * @param listener
     */
    public void setOnClearSearchActionListener(OnClearSearchActionListener listener) {
        this.mOnClearSearchActionListener = listener;
    }

    private void openMenuDrawable(final DrawerArrowDrawable drawerArrowDrawable, boolean withAnim) {
        if (withAnim) {
            ValueAnimator anim = ValueAnimator.ofFloat(0.0f, 1.0f);
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {

                    float value = (Float) animation.getAnimatedValue();
                    drawerArrowDrawable.setProgress(value);
                }
            });
            anim.setDuration(MENU_ICON_ANIM_DURATION);
            anim.start();
        } else {
            drawerArrowDrawable.setProgress(1.0f);
        }
    }

    private void closeMenuDrawable(final DrawerArrowDrawable drawerArrowDrawable, boolean withAnim) {
        if (withAnim) {
            ValueAnimator anim = ValueAnimator.ofFloat(1.0f, 0.0f);
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {

                    float value = (Float) animation.getAnimatedValue();
                    drawerArrowDrawable.setProgress(value);
                }
            });
            anim.setDuration(MENU_ICON_ANIM_DURATION);
            anim.start();
        } else {
            drawerArrowDrawable.setProgress(0.0f);
        }
    }

    private boolean isRTL() {

        Configuration config = getResources().getConfiguration();
        return ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.isFocused = mIsFocused;
        savedState.query = getQuery();
        savedState.searchHint = mSearchHint;
        savedState.showSearchKey = mShowSearchKey;
        savedState.isTitleSet = mIsTitleSet;
        savedState.queryTextColor = mSearchInputTextColor;
        savedState.searchHintTextColor = mSearchInputHintColor;
        savedState.actionOverflowMenuColor = mOverflowIconColor;
        savedState.menuItemIconColor = mActionMenuItemColor;
        savedState.leftIconColor = mLeftActionIconColor;
        savedState.clearBtnColor = mClearBtnColor;
        savedState.menuId = mMenuId;
        savedState.leftActionMode = mLeftActionMode;
        savedState.queryTextSize = mQueryTextSize;
        savedState.dismissOnSoftKeyboardDismiss = mCloseSearchOnSoftKeyboardDismiss;
        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        final SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        mIsFocused = savedState.isFocused;
        mIsTitleSet = savedState.isTitleSet;
        mMenuId = savedState.menuId;
        mOldQuery = savedState.query;
        setSearchText(mOldQuery);
        setShowSearchKey(savedState.showSearchKey);
        setSearchHint(savedState.searchHint);
        setQueryTextColor(savedState.queryTextColor);
        setQueryTextSize(savedState.queryTextSize);
        setHintTextColor(savedState.searchHintTextColor);
        setActionMenuOverflowColor(savedState.actionOverflowMenuColor);
        setMenuItemIconColor(savedState.menuItemIconColor);
        setLeftActionIconColor(savedState.leftIconColor);
        setClearBtnColor(savedState.clearBtnColor);
        setLeftActionMode(savedState.leftActionMode);
        setCloseSearchOnKeyboardDismiss(savedState.dismissOnSoftKeyboardDismiss);

        if (mIsFocused) {
            mSkipTextChangeEvent = true;
            mSkipQueryFocusChangeEvent = true;

            mClearButton.setVisibility((savedState.query.length() == 0) ? View.INVISIBLE : View.VISIBLE);
            mLeftAction.setVisibility(View.VISIBLE);

            Util.showSoftKeyboard(getContext(), mSearchInput);
        }
    }

    static class SavedState extends BaseSavedState {

        private boolean isFocused;
        private String query;
        private int queryTextSize;
        private String searchHint;
        private boolean showSearchKey;
        private boolean isTitleSet;
        private int queryTextColor;
        private int searchHintTextColor;
        private int actionOverflowMenuColor;
        private int menuItemIconColor;
        private int leftIconColor;
        private int clearBtnColor;
        private int menuId;
        private int leftActionMode;
        private long suggestionsSectionAnimSuration;
        private boolean dismissOnSoftKeyboardDismiss;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            isFocused = (in.readInt() != 0);
            query = in.readString();
            queryTextSize = in.readInt();
            searchHint = in.readString();
            showSearchKey = (in.readInt() != 0);
            isTitleSet = (in.readInt() != 0);
            queryTextColor = in.readInt();
            searchHintTextColor = in.readInt();
            actionOverflowMenuColor = in.readInt();
            menuItemIconColor = in.readInt();
            leftIconColor = in.readInt();
            clearBtnColor = in.readInt();
            menuId = in.readInt();
            leftActionMode = in.readInt();
            suggestionsSectionAnimSuration = in.readLong();
            dismissOnSoftKeyboardDismiss = (in.readInt() != 0);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(isFocused ? 1 : 0);
            out.writeString(query);
            out.writeInt(queryTextSize);
            out.writeString(searchHint);
            out.writeInt(showSearchKey ? 1 : 0);
            out.writeInt(isTitleSet ? 1 : 0);
            out.writeInt(queryTextColor);
            out.writeInt(searchHintTextColor);
            out.writeInt(actionOverflowMenuColor);
            out.writeInt(menuItemIconColor);
            out.writeInt(leftIconColor);
            out.writeInt(clearBtnColor);
            out.writeInt(menuId);
            out.writeInt(leftActionMode);
            out.writeLong(suggestionsSectionAnimSuration);
            out.writeInt(dismissOnSoftKeyboardDismiss ? 1 : 0);
        }

        public static final Creator<SavedState> CREATOR
                = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    private DrawerLayout.DrawerListener mDrawerListener = new DrawerListener();

    public void attachNavigationDrawerToMenuButton(@NonNull DrawerLayout drawerLayout) {
        drawerLayout.addDrawerListener(mDrawerListener);
        setOnLeftMenuClickListener(new NavDrawerLeftMenuClickListener(drawerLayout));
    }

    public void detachNavigationDrawerFromMenuButton(@NonNull DrawerLayout drawerLayout) {
        drawerLayout.removeDrawerListener(mDrawerListener);
        setOnLeftMenuClickListener(null);
    }

    private class DrawerListener implements DrawerLayout.DrawerListener {
        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
            setMenuIconProgress(slideOffset);
        }

        @Override
        public void onDrawerOpened(View drawerView) {

        }

        @Override
        public void onDrawerClosed(View drawerView) {

        }

        @Override
        public void onDrawerStateChanged(int newState) {

        }
    }

    private class NavDrawerLeftMenuClickListener implements OnLeftMenuClickListener {

        DrawerLayout mDrawerLayout;

        public NavDrawerLeftMenuClickListener(DrawerLayout drawerLayout) {
            mDrawerLayout = drawerLayout;
        }

        @Override
        public void onMenuOpened() {
            mDrawerLayout.openDrawer(GravityCompat.START);
        }

        @Override
        public void onMenuClosed() {
            //do nothing
        }
    }
}
