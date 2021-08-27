package com.osfans.trime.ime.SymbolKeyboard;


import android.content.Context;
import android.view.inputmethod.InputConnection;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.osfans.trime.R;
import com.osfans.trime.ime.core.Trime;
import com.osfans.trime.ime.enums.SymbolKeyboardType;
import com.osfans.trime.setup.Config;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class LiquidKeyboard {
    private final Context context;

    private RecyclerView keyboardView;
    private LinearLayout parentView;
    private ClipboardAdapter mClipboardAdapter;
    private SimpleAdapter simpleAdapter;
    private List<SimpleKeyBean> clipboardBeanList;
    private List<SimpleKeyBean> simpleKeyBeans;
    private int margin_x, margin_top, single_width, parent_width;

    private int keyHeight;
    private boolean isLand;

    public void setView(RecyclerView view) {
        keyboardView = view;
    }


    public void setLand(boolean land) {
        isLand = land;
    }


    public LiquidKeyboard(Context context) {
        this.context = context;

        clipboardBeanList = ClipboardDao.get().getAllSimpleBean();
        Timber.d("clipboardBeanList.size=%s", clipboardBeanList.size());
    }


    public void addClipboardData(String text) {
        ClipboardBean bean = new ClipboardBean(text);
        ClipboardDao.get().add(bean);
        clipboardBeanList.add(0, bean);
        if (mClipboardAdapter != null)
            mClipboardAdapter.notifyItemInserted(0);
    }

    public void select(int i) {
        TabTag tag = TabManager.getTag(i);
        calcPadding(tag.type);
        if (tag.type == SymbolKeyboardType.CLIPBOARD) {
            TabManager.get().select(i);
            initClipboardData();
        } else {
            initFixData(i);
        }
    }

    // 设置liquidKeyboard共用的布局参数
    public void calcPadding(int width) {

        Config config = Config.get(context);
        parent_width = width;
        final Map<?, ?> liquid_config = config.getLiquidKeyboard();

        // liquid_keyboard/margin_x定义了每个键左右两边的间隙，也就是说相邻两个键间隙是x2，而horizontal_gap定义的是spacer，使用时需要/2
        if (liquid_config != null) {
            if (liquid_config.containsKey("margin_x")) {
                Object o = Config.getPixel(liquid_config, "margin_x", 0);
                margin_x = o == null ? 0 : (int) o;
            }
        }

        if (margin_x == 0) {
            int horizontal_gap = config.getPixel("horizontal_gap");
            if (horizontal_gap > 1) {
                horizontal_gap = horizontal_gap / 2;
            }
            margin_x = horizontal_gap;
        }

        // 初次显示布局，需要刷新背景
        parentView = (LinearLayout) keyboardView.getParent();
        parentView.setBackground(config.getLiquidDrawable("keyboard_back_color", context));

        keyHeight = config.getLiquidPixel("key_height_land");
        if (!isLand || keyHeight <= 0)
            keyHeight = config.getLiquidPixel("key_height");
        margin_top = config.getLiquidPixel("vertical_gap");

        Timber.i("config keyHeight=" + keyHeight + " marginTop=" + margin_top);

        if (isLand)
            single_width = config.getLiquidPixel("single_width_land");
        if (single_width <= 0)
            single_width = config.getLiquidPixel("single_width");
        if (single_width <= 0)
            single_width = context.getResources().getDimensionPixelSize(R.dimen.simple_key_single_width);
    }

    // 每次点击tab都需要刷新的参数
    private void calcPadding(SymbolKeyboardType type) {

        Config config = Config.get(context);
        int padding = config.getPixel("keyboard_padding");

        if (type == SymbolKeyboardType.SINGLE) {
            padding = (parentView.getWidth() > 0 ? parentView.getWidth() : parent_width) % (single_width + margin_x * 2) / 2;
        }

        Timber.d("set_keyboard_padding=%s / %s", padding, parentView.getWidth());
        if (padding > 0)
            parentView.setPadding(padding, 0, padding, 0);

    }

    public void initFixData(int i) {
        keyboardView.removeAllViews();
        mClipboardAdapter = null;
        //设置布局管理器
        FlexboxLayoutManager flexboxLayoutManager = new FlexboxLayoutManager(context);
        //flexDirection 属性决定主轴的方向（即项目的排列方向）。类似 LinearLayout 的 vertical 和 horizontal。
        flexboxLayoutManager.setFlexDirection(FlexDirection.ROW);//主轴为水平方向，起点在左端。
        //flexWrap 默认情况下 Flex 跟 LinearLayout 一样，都是不带换行排列的，但是flexWrap属性可以支持换行排列。
        flexboxLayoutManager.setFlexWrap(FlexWrap.WRAP);//按正常方向换行
        //justifyContent 属性定义了项目在主轴上的对齐方式。
        flexboxLayoutManager.setJustifyContent(JustifyContent.FLEX_START);//交叉轴的起点对齐。

        keyboardView.removeAllViews();
        keyboardView.setLayoutManager(flexboxLayoutManager);

        //设置适配器
        simpleKeyBeans = TabManager.get().select(i);
        simpleAdapter = new SimpleAdapter(context, simpleKeyBeans);

        simpleAdapter.configStyle(single_width, keyHeight, margin_x, margin_top);
//            simpleAdapter.configKey(single_width,height,margin_x,margin_top);
        keyboardView.setAdapter(simpleAdapter);
        //添加分割线
        //设置添加删除动画
        //调用ListView的setSelected(!ListView.isSelected())方法，这样就能及时刷新布局
        keyboardView.setSelected(true);

        //列表适配器的点击监听事件
        simpleAdapter.setOnItemClickLitener((view, position) -> {
            InputConnection ic = Trime.getService().getCurrentInputConnection();
            if (ic != null) {
                ic.commitText(simpleKeyBeans.get(position).getText(), 1);
            }
        });
    }


    public void initClipboardData() {
        keyboardView.removeAllViews();
        simpleAdapter = null;

        //设置布局管理器
        FlexboxLayoutManager flexboxLayoutManager = new FlexboxLayoutManager(context);
        //flexDirection 属性决定主轴的方向（即项目的排列方向）。类似 LinearLayout 的 vertical 和 horizontal。
        flexboxLayoutManager.setFlexDirection(FlexDirection.ROW);//主轴为水平方向，起点在左端。
        //flexWrap 默认情况下 Flex 跟 LinearLayout 一样，都是不带换行排列的，但是flexWrap属性可以支持换行排列。
        flexboxLayoutManager.setFlexWrap(FlexWrap.WRAP);//按正常方向换行
        //justifyContent 属性定义了项目在主轴上的对齐方式。
        flexboxLayoutManager.setJustifyContent(JustifyContent.FLEX_START);//交叉轴的起点对齐。
//            flexboxLayoutManager.setAlignItems(AlignItems.BASELINE);
        keyboardView.setLayoutManager(flexboxLayoutManager);

        clipboardBeanList = ClipboardDao.get().getAllSimpleBean();
        mClipboardAdapter = new ClipboardAdapter(context, clipboardBeanList);


        mClipboardAdapter.configStyle(margin_x, margin_top);

        keyboardView.setAdapter(mClipboardAdapter);
        //调用ListView的setSelected(!ListView.isSelected())方法，这样就能及时刷新布局
        keyboardView.setSelected(true);

        mClipboardAdapter.setOnItemClickLitener((view, position) -> {
            InputConnection ic = Trime.getService().getCurrentInputConnection();
            if (ic != null) {
                ic.commitText(clipboardBeanList.get(position).getText(), 1);
            }
        });

    }

}