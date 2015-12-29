/**************************************************************************************************
 * Copyright (c) 2016-present, Produvia, LTD.
 * All rights reserved.
 * This source code is licensed under the MIT license
 **************************************************************************************************/
package produvia.com.lights;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**************************************************************************************************
 * The ColorSchemeFragment Fragment contains a simple listview holding various bitmaps
 * Whenever a user presses a bitmap - we collect the palette and set the light service colors
 * according to the selected palette
 **************************************************************************************************/
public class ColorSchemeFragment extends Fragment implements CustomRecyclerAdapter.CustomListCallbacks {

    class BitmapHolder{
        public BitmapHolder(Integer res, String title){
            mBitmapResource = res;
            mTitle = title;
        }
        Integer mBitmapResource;
        String mTitle;
    }

    private ArrayList<BitmapHolder>mBitmapResources;

    private RecyclerView mRecyclerView;
    private ImageAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;




    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ColorSchemeFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set up the pics listview:
        mBitmapResources = new ArrayList<>();
        mBitmapResources.add(new BitmapHolder(R.drawable.cs_aurora, ""));
        mBitmapResources.add(new BitmapHolder(R.drawable.cs_cicada, ""));
        mBitmapResources.add(new BitmapHolder(R.drawable.cs_desert, ""));
        mBitmapResources.add(new BitmapHolder(R.drawable.cs_drip, ""));
        mBitmapResources.add(new BitmapHolder(R.drawable.cs_rainforest, ""));
        mBitmapResources.add(new BitmapHolder(R.drawable.cs_mediteranian, ""));
        mBitmapResources.add(new BitmapHolder(R.drawable.cs_savana, ""));
        mBitmapResources.add(new BitmapHolder(R.drawable.cs_sea, ""));
        mBitmapResources.add(new BitmapHolder(R.drawable.cs_vintage, ""));
    }
    
    

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //setup the recycler view:
        View view = inflater.inflate(R.layout.fragment_color_scheme, container, false);
        mRecyclerView = (RecyclerView)view.findViewById(R.id.propertiesList);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new ImageAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
        return view;
    }



    @Override
    public void onColorChanged(CustomListItem item) {}
    @Override
    public void onItemClicked(CustomListItem item) {}
    @Override
    public void onToggleClicked(CustomListItem item, boolean value) {}
    @Override
    public void onLeftImageClicked(CustomListItem item) {}


    //in this case the viewHolder is a simple view with a bitmap:
    class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

        class ViewHolder extends RecyclerView.ViewHolder{

            public TextView txtTitle;
            public ImageView imageView;

            public ViewHolder(View itemView) {
                super(itemView);
                txtTitle = (TextView) itemView.findViewById(R.id.title);
                imageView = (ImageView) itemView.findViewById(R.id.img);
            }
        }

        @Override
        public ImageAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_item, parent, false);
            ViewHolder viewHolder = new ViewHolder(v);
            return viewHolder;

        }

        /*******************************************************************
         *
         * This function gets called on touch events
         *
         * It generates a color palette according to the area on the
         * bitmap that was pressed
         *******************************************************************/
        public void setColorsByBitmap(int position, MotionEvent event){
            int itemPosition = position;
            BitmapHolder item = mBitmapResources.get(itemPosition);
            //need to setup the palette
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), item.mBitmapResource);

            int x = (int) event.getX();
            int y = (int) event.getY();


            int width  = 100;
            int height = 100;
            if(x+width >= bmp.getWidth())
                width = bmp.getWidth() - (x+1);
            if(y+height >= bmp.getHeight())
                height = bmp.getHeight() - (y+1);

            Bitmap bitmap_at_area = Bitmap.createBitmap(bmp, x, y, width, height);
            Palette p = Palette.from(null).generate(bitmap_at_area);
            List<Palette.Swatch> swatches = p.getSwatches();
            List<Palette.Swatch> reordered_swatches = swatches;

            //gather all the lights under this specific ssid and toggle them:
            int swatch_count= 0;

            ArrayList <String> ids_triggered = new ArrayList<>();

            for(int i = 0; i < SmartLightsActivity.mIotLightServices.size(); i++){

                CustomListItem obj= SmartLightsActivity.mIotLightServices.get(i);
                if(obj.isToggleEnabled() == true)
                    obj.setToggle(true);

                if(!obj.getClass().equals(LightService.class))
                    continue;
                LightService light_color_service = (LightService)obj;
                boolean triggered = false;
                for(int j = 0; j < ids_triggered.size(); j++){
                    if(ids_triggered.get(j).equals(light_color_service.getId())){
                        triggered = true;
                        break;
                    }

                }
                if(!triggered) {
                    //add the service id:
                    ids_triggered.add(light_color_service.getId());
                }


                int swatch_idx = (swatch_count%reordered_swatches.size());
                if((i%2)==1)
                    swatch_idx = (reordered_swatches.size()-(1+swatch_idx));
                int rgb =reordered_swatches.get(swatch_idx).getRgb();

                swatch_count++;

                LightService lightService = (LightService)obj;

                lightService.setColor(rgb);
                lightService.onColorChanged(false);
                lightService.onPowerChanged(true, !triggered);
            }
        }

        @Override
        public void onBindViewHolder(ImageAdapter.ViewHolder holder, final int position) {
            BitmapHolder item = mBitmapResources.get(position);
            holder.imageView.setImageResource(item.mBitmapResource);
            holder.txtTitle.setText(item.mTitle);


            holder.imageView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getActionMasked() == MotionEvent.ACTION_DOWN)
                        setColorsByBitmap(position, event);
                    return true;
                }
            });

        }

        @Override
        public int getItemCount() {
            return mBitmapResources.size();
        }
    }
}

