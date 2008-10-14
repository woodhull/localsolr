package com.pjaol.client;

import com.mapitz.gwt.googleMaps.client.GControl;
import com.mapitz.gwt.googleMaps.client.GLatLng;
import com.mapitz.gwt.googleMaps.client.GMap2;
import com.mapitz.gwt.googleMaps.client.GMap2Widget;
import com.mapitz.gwt.googleMaps.client.GMarkerEventManager;

public class MapModule {

	GMap2 gmap;
	GMap2Widget mapWidget;
	GMarkerEventManager gmem = new GMarkerEventManager();

	public GMap2Widget getMap() {
		mapWidget = new GMap2Widget();
		gmap = mapWidget.getGmap();
		mapWidget.setHeight("500");
		mapWidget.setWidth("500");

		gmap.setCenter(new GLatLng(50, -98), 3);

		gmap.addControl(GControl.GMapTypeControl());
		gmap.addControl(GControl.GOverviewMapControl());
		gmap.addControl(GControl.GSmallMapControl());
		gmap.addControl(GControl.GScaleControl());
		mapWidget.setWidth("100%");
		return mapWidget;
	}
}
