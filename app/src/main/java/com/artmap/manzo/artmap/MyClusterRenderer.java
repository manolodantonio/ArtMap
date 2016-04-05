package com.artmap.manzo.artmap;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

/**
 * Created by Manzo on 07/02/2015.
 */
public class MyClusterRenderer<T extends MyClusterItem> extends DefaultClusterRenderer<T> {


    public MyClusterRenderer(Context context, GoogleMap map, ClusterManager<T> clusterManager) {
        super(context, map, clusterManager);
    }

    @Override
    protected boolean shouldRenderAsCluster(Cluster<T> cluster) {
        return cluster.getSize() > 2;
    }

    @Override
    protected void onBeforeClusterItemRendered(T item, MarkerOptions markerOptions) {
        super.onBeforeClusterItemRendered(item, markerOptions);
        markerOptions
                .title(item.getmTitle())
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.artmark))
                .snippet(String.valueOf(item.getmSnippet()));


    }


    @Override
    public T getClusterItem(Marker marker) {
        return super.getClusterItem(marker);
    }

    @Override
    public Marker getMarker(T clusterItem) {
        return super.getMarker(clusterItem);
    }



}
