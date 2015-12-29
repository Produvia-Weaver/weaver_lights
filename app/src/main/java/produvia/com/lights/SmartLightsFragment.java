/**************************************************************************************************
 * Copyright (c) 2016-present, Produvia, LTD.
 * All rights reserved.
 * This source code is licensed under the MIT license
 **************************************************************************************************/
package produvia.com.lights;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * A list fragment representing a list of light services.
 */
public class SmartLightsFragment extends Fragment implements CustomRecyclerAdapter.CustomListCallbacks {


    private View mLoadingProgressBar;

    static Callbacks mCallbacks;
    private RecyclerView mRecyclerView;
    private CustomRecyclerAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private TextView mProgressMessage;



    @Override
    public void onColorChanged(CustomListItem colorItem) {

        //if it's the master switch turn all lights on or off - otherwise just toggle the specific
        //light pressed:
        int color = colorItem.getNewColor();
        if(colorItem instanceof LightService) {
            colorItem.setColor(color);
            colorItem.onColorChanged(true);
        }
        else {
            //set the master switch color:
            colorItem.setColor(color);
            //gather all the lights under this specific network and toggle them:
            ArrayList <String> ids_triggered = new ArrayList<>();
            ArrayList<LightService> services = new ArrayList<>();
            for (int i = 0; i < SmartLightsActivity.mIotLightServices.size(); i++) {

                CustomListItem item = SmartLightsActivity.mIotLightServices.get(i);
                if (!item.getClass().equals(LightService.class))
                    continue;
                LightService light_color_service = (LightService) item;
                boolean triggered = false;
                for (int j = 0; j < ids_triggered.size(); j++) {
                    if (ids_triggered.get(j).equals(light_color_service.getId())) {
                        triggered = true;
                        break;
                    }

                }
                light_color_service.setColor(color);
                light_color_service.onColorChanged(!triggered);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public void onItemClicked(CustomListItem item) {
        if(item instanceof LightService){
            item.onClick();
            notifyDataSetChanged();
            return;
        }
        mCallbacks.onItemSelected(item);
    }

    @Override
    public void onToggleClicked(CustomListItem toggleItem, boolean value) {
        int color = toggleItem.getNewColor();
        //if it's the master switch turn all lights on or off - otherwise just toggle the specific
        //light pressed:
        if(toggleItem instanceof LightService) {

            toggleItem.setColor(color);
            if(value)
                toggleItem.onColorChanged(false);
            ((LightService) toggleItem).onPowerChanged(value, true);

        }
        else {
            toggleItem.setColor(color);
            //gather all the lights under this specific network and toggle them:
            ArrayList <String> ids_triggered = new ArrayList<>();
            ArrayList<LightService> services = new ArrayList<>();

            for (int i = 0; i < SmartLightsActivity.mIotLightServices.size(); i++) {

                CustomListItem item = SmartLightsActivity.mIotLightServices.get(i);
                if (!item.getClass().equals(LightService.class))
                    continue;
                LightService light_color_service = (LightService) item;
                boolean triggered = false;
                for (int j = 0; j < ids_triggered.size(); j++) {
                    if (ids_triggered.get(j).equals(light_color_service.getId())) {
                        triggered = true;
                        break;
                    }

                }
                light_color_service.setColor(color);
                if(value)
                    light_color_service .onColorChanged(false);
                light_color_service.onPowerChanged(value, !triggered);
            }
        }
        notifyDataSetChanged();
    }



    private void notifyDataSetChanged(){
        if(mAdapter == null)
            return;
        new Thread(){
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                });

            }
        }.start();
    }


    @Override
    public void onLeftImageClicked(CustomListItem item) {
        onItemClicked(item);
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        void onItemSelected(CustomListItem hub);
        void onViewCreated(CustomRecyclerAdapter adapter);
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(CustomListItem c) {
        }

        @Override
        public void onViewCreated(CustomRecyclerAdapter adapter) {

        }
    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SmartLightsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_smart_lights, container, false);

        mLoadingProgressBar = view.findViewById(R.id.custom_horizontal_progressbar);
        mProgressMessage = ((TextView)view.findViewById(R.id.progressbar_message));
        mProgressMessage.setText("Scanning for light services...");


        mRecyclerView = (RecyclerView)view.findViewById(R.id.categorylist);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter:
        mAdapter = new CustomRecyclerAdapter(mRecyclerView, getActivity(), SmartLightsActivity.mIotLightServices);
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (SmartLightsActivity.mIotLightServices!= null && SmartLightsActivity.mIotLightServices.size() > 0)
                    mLoadingProgressBar.setVisibility(View.GONE);

            }

        });
        mRecyclerView.setAdapter(mAdapter);

        mCallbacks.onViewCreated(mAdapter);
        return view;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks.onViewCreated(null);
        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onPause() {
        mAdapter.mCallbacks = null;
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.mCallbacks = this;
        showError();
    }

    public void showError(){
        if(!SmartLightsActivity.mErrorOccurred)
            return;
        View view = getView();
        if( view == null)
            return;
        TextView pmessage = ((TextView) view.findViewById(R.id.progressbar_message));
        ImageView lights_not_found = (ImageView) view.findViewById(R.id.lights_not_found);
        View progress_spinner = view.findViewById(R.id.progressbar);
        if (pmessage != null) {
            pmessage.setText(SmartLightsActivity.mErrorMessage);
            pmessage.setTextSize(20);
        }
        if (progress_spinner != null)
            progress_spinner.setVisibility(View.GONE);

        if (lights_not_found != null)
            lights_not_found.setVisibility(View.VISIBLE);

    }


}
