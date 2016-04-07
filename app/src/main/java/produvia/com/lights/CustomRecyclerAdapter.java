/**************************************************************************************************
 * Copyright (c) 2016-present, Produvia, LTD.
 * All rights reserved.
 * This source code is licensed under the MIT license
 **************************************************************************************************/

package produvia.com.lights;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.SVBar;
import com.larswerkman.holocolorpicker.ValueBar;

import java.util.ArrayList;




public class CustomRecyclerAdapter extends RecyclerView.Adapter<CustomRecyclerAdapter.ViewHolder> {

    public static final int VIEW_TYPE_TITLE = 0;
    public static final int VIEW_TYPE_NORMAL = 1;
    private static final int[] mColorsArray= new int[] {0xff99cc00, 0xffff8800, 0xff0099cc, 0xffcc0000};

    public interface CustomListCallbacks {
        void onColorChanged(CustomListItem cli);
        void onItemClicked(CustomListItem item, View v, int position);
        void onToggleClicked(CustomListItem item, boolean checked);
        void onLeftImageClicked(CustomListItem item, View v, int position);
    }

    public CustomListCallbacks mCallbacks;



    class ViewHolder extends RecyclerView.ViewHolder{

        public TextView txtTitle;
        public ImageView leftImageView;
        public TextView detailsTextView;
        public View colorPicker;
        public View saturation_brightness_bar;
        public View value_bar;
        public android.widget.ToggleButton toggleButton;

        public ViewHolder(View itemView) {
            super(itemView);
            txtTitle = (TextView) itemView.findViewById(R.id.title);
            leftImageView = (ImageView) itemView.findViewById(R.id.img);

            detailsTextView = (TextView) itemView.findViewById(R.id.description);
            colorPicker = itemView.findViewById(R.id.color_picker);
            saturation_brightness_bar = itemView.findViewById(R.id.saturation_brightness_bar);
            value_bar = itemView.findViewById(R.id.value_bar);
            toggleButton = (android.widget.ToggleButton)itemView.findViewById(R.id.toggleButton);
        }
    }



    @Override
    public int getItemViewType(int position) {
        //Implement your logic here
        CustomListItem item = mArray.get(position);

        return item.isTitle()?VIEW_TYPE_TITLE:VIEW_TYPE_NORMAL;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        if(viewType == VIEW_TYPE_NORMAL) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_list_item_card, parent, false);
        } else {

            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_list_item_title_card, parent, false);
        }
        ViewHolder viewHolder = new ViewHolder(v);

        return viewHolder;

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        CustomListItem item = mArray.get(position);
        holder.itemView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int itemPosition = position;
                CustomListItem item = CustomRecyclerAdapter.this.mArray.get(itemPosition);
                if (mCallbacks != null)
                    mCallbacks.onItemClicked(item, v, itemPosition);
            }
        });

        holder.leftImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int itemPosition = position;
                CustomListItem item = CustomRecyclerAdapter.this.mArray.get(itemPosition);
                if (mCallbacks != null)
                    mCallbacks.onLeftImageClicked(item, v, itemPosition);
            }
        });
        //holder.leftImageView.setClickable(left_image_clickable && mListView.getCheckedItemCount()==0);


        boolean initPicker = false;
        int colorPickerVisibility = View.GONE;
        int satBriVisibility = View.GONE;
        int valueVisibility = View.GONE;

        if (item.isColorPickerEnabled()) {
            colorPickerVisibility = View.VISIBLE;
            satBriVisibility = View.VISIBLE;
            initPicker = true;
        }else if(item.isDimmerEnabled()) {
            valueVisibility = View.VISIBLE;
            initPicker = true;
        }


        if(holder.colorPicker != null)
            holder.colorPicker.setVisibility(colorPickerVisibility);
        if(holder.saturation_brightness_bar != null)
            holder.saturation_brightness_bar.setVisibility(satBriVisibility);
        if(holder.value_bar != null)
            holder.value_bar.setVisibility(valueVisibility);
        if(initPicker)
            initializeColorPicker(holder.itemView, item);



        if(item.isToggleEnabled()) {
            holder.toggleButton.setVisibility(View.VISIBLE);
            holder.toggleButton.setChecked(item.getToggle());
            final CustomListItem fitem = item;
            holder.toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    fitem.setToggle( isChecked );
                    if(mCallbacks != null)
                        mCallbacks.onToggleClicked(fitem, isChecked);

                }
            });
        }

        //holder.itemView.setBackgroundColor(item.getColor());

        String name = item.getName();
        String description = item.getDescription();

        if(getItemViewType(position) == VIEW_TYPE_TITLE)
            holder.txtTitle.setTextColor(mColorsArray[position%mColorsArray.length]);

        if(name != null)
            holder.txtTitle.setText(name);
        if(description != null && holder.detailsTextView != null)
            holder.detailsTextView.setText(description);
        Integer image = item.getLeftImage();
        if( image != null ) {
            holder.leftImageView.setImageResource(image);
            //holder.leftImageView.setBackgroundColor(item.getColor());
        }
    }

    @Override
    public int getItemCount() {
        return mArray.size();
    }


    ArrayList<CustomListItem> mArray = null;
    RecyclerView mListView = null;







    public CustomRecyclerAdapter(RecyclerView lv, Activity context,
                                 ArrayList<CustomListItem> dataArray) {
        super();
        mListView = lv;
        mArray = dataArray;
    }





    private void initializeColorPicker(View convertView, final CustomListItem item) {

        ColorPicker picker = (ColorPicker) convertView.findViewById(R.id.color_picker);
        SVBar svBar = (SVBar) convertView.findViewById(R.id.saturation_brightness_bar);
        ValueBar valueBar = (ValueBar) convertView.findViewById(R.id.value_bar);
        //SVBar saturationBar = (SVBar) convertView.findViewById(R.id.saturationbar);

        //picker.addSaturationBar(saturationBar);
        //picker.addValueBar(valueBar);
        picker.addSVBar(svBar);
        picker.addValueBar(valueBar);

        //To get the color
        int color = item.getColor();
        picker.setColor(color);

        svBar.setColor(color);
        valueBar.setColor(color);

        picker.setOldCenterColor(picker.getColor());

        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_UP){
                    if(item.getColor()!= item.getNewColor()) {
                        item.setColor(item.getNewColor());
                        if(mCallbacks != null)
                            mCallbacks.onColorChanged(item);
                    }
                }
                return false;
            }
        };

        picker.setOnTouchListener(touchListener);
        svBar.setOnTouchListener(touchListener);
        valueBar.setOnTouchListener(touchListener);


        // adds listener to the colorpicker which is implemented
        //in the activity
        picker.setOnColorChangedListener(new ColorPicker.OnColorChangedListener() {
            @Override
            public void onColorChanged(int color) {
                item.setNewColor(color);
            }
        });

        //to turn off showing the old color
        picker.setShowOldCenterColor(false);

    }



}