/**************************************************************************************************
 * Copyright (c) 2016-present, Produvia, LTD.
 * All rights reserved.
 * This source code is licensed under the MIT license
 **************************************************************************************************/
package produvia.com.lights;

public class CustomListItem {
    private String mName;
    private Integer  mImg;
    private String mDescription;
    private boolean mIsTitle = false;
    private boolean mIsToggleEnabled = false;
    private boolean mToggleState = false;
    private boolean mIsColorPickerEnabled = false;
    private boolean mIsDimmerEnabled = false;

    private int mColor = 0xffffffff;
    private int mNewColor = 0xffffffff;



    public CustomListItem(String name, String description,
                          Integer img, boolean isTitle,
                          boolean isToggleEnabled) {

        mName = name;
        mDescription = description;
        mImg = img;
        mIsTitle = isTitle;
        mIsToggleEnabled = isToggleEnabled;
        mIsColorPickerEnabled = false;
        mIsDimmerEnabled = false;

    }

    public String getName(){
        return mName;
    }

    public void setName(String name){
        mName = name;
    }

    public boolean isTitle(){
        return mIsTitle;
    }

    public String getDescription(){
        return mDescription;
    }

    public void setDescription(String description){
        mDescription = description;
    }

    public Integer getColor(){
        return mColor;
    }

    public void setColor(int color){
        mColor = color;
    }


    public Integer getNewColor(){
        return mNewColor;
    }

    public void setNewColor(int color){
        mNewColor = color;
    }

    public void setLeftImage(int image){
        mImg = image;
    }


    public Integer getLeftImage(){
        return mImg;
    }


    public void setColorPickerEnabled(boolean enable){
        mIsColorPickerEnabled = enable;
    }

    public boolean isColorPickerEnabled(){
        return mIsColorPickerEnabled;
    }

    public void setDimmerEnabled(boolean enable){
        mIsDimmerEnabled = enable;
    }
    public boolean isDimmerEnabled(){
        return mIsDimmerEnabled;
    }

    public boolean isToggleEnabled(){
        return mIsToggleEnabled;
    }

    public void toggle() {
        mToggleState = !mToggleState;
    }

    public void setToggle(boolean state) {
        mToggleState = state;
    }
    public boolean getToggle() {
        return mToggleState;
    }

    public void onClick(){

    }


    public void onColorChanged(boolean commitChanges) {
    }
}
