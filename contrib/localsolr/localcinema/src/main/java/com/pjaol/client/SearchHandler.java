package com.pjaol.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.mapitz.gwt.googleMaps.client.GClientGeocoder;
import com.mapitz.gwt.googleMaps.client.GEvent;
import com.mapitz.gwt.googleMaps.client.GEventHandler;
import com.mapitz.gwt.googleMaps.client.GGeocodeResultListener;
import com.mapitz.gwt.googleMaps.client.GLatLng;
import com.mapitz.gwt.googleMaps.client.GMarker;
import com.mapitz.gwt.googleMaps.client.GMarkerEventClickListener;
import com.mapitz.gwt.googleMaps.client.JSObject;


public class SearchHandler implements JSONRequestHandler{


	private MapModule map;
	private GClientGeocoder geocoder = new GClientGeocoder();
	
	private SearchFormModule searchForm;
	private GLatLng latlngPoint;
	private String url = "http://localhost:8983/solr/select?qt=geo&wt=json";
	private Panel results;
	private boolean enableDragSearch =false;
	private String searchUrl;
	
	public SearchHandler (MapModule map, SearchFormModule searchForm){
		
		this.map = map;
		this.searchForm = searchForm;
	}

	public void search() {
		final String query = searchForm.query.getText();
		String address = searchForm.address.getText();
		
		//Window.alert("Query: "+ query + " Address: "+ address);
		geocoder.getLatLng(address, new GGeocodeResultListener(){
			public void onSuccess(GLatLng latlng) {
				map.gmap.setCenter(latlng, 12);
				latlngPoint = latlng;
				enableDragSearch = true;
				getJSON(query);
			}

			public void onFail(String address) {
				Window.alert("Address: "+ address +" Failed to recognize");
				
			}
		});
		
		if (! enableDragSearch){
			GEvent.addListener(map.gmap, "moveend", new GEventHandler() {

				public void onHandle(JSObject source, JSObject[] param) {
					if (enableDragSearch) {
						latlngPoint = map.gmap.getCenter();
						map.gmap.clearOverlays();
						getJSON(searchForm.query.getText());
					}
				}
				
			});
			enableDragSearch = true;
		}
		
	}
	
	public void getJSON(String query){
	
		String req = url +"&q="+query
					+"&lat="+latlngPoint.lat()
					+"&long="+latlngPoint.lng()
					+"&radius=1&json.wrf=";
		searchUrl = url +"&q="+query
					+"&lat="+latlngPoint.lat()
					+"&long="+latlngPoint.lng()
					+"&radius=1";
		
		JSONRequest.get(req, this);
		
		
	}

	public void onRequestComplete(JavaScriptObject json) {
	
		JSONObject jobject = new JSONObject(json);
		VerticalPanel searchResults = new VerticalPanel();
		JSONObject respHeader = ((JSONObject)jobject.get("responseHeader"));
		JSONObject resp = ((JSONObject)jobject.get("response"));
		JSONArray docs = ((JSONArray) resp.get("docs"));
		
		Label title = new Label("Results Found: "+resp.get("numFound").toString() +" at ("+
				latlngPoint.lat()+", "+ latlngPoint.lng()+")");
		searchResults.add(title);
		
		int sz = docs.size();
		for (int i = 0; i < sz; i++){
			JSONObject doc = (JSONObject)docs.get(i);
			final String cname = doc.get("cinema").toString().replace('"', ' ').trim();
			final String street = doc.get("street").toString().replace('"', ' ').trim();
			final String city = doc.get("city").toString().replace('"', ' ').trim();
			final String zip = doc.get("zip").toString().replace('"', ' ').trim();
			final String distance = doc.get("geo_distance").toString().replace('"', ' ').trim();
			
			Label r = new Label(cname);
			r.setStylePrimaryName("resTitle");
			
			HTML h = new HTML("<address>"+ street +", "+ city+", "+ zip+"</address>"+" "+ distance +" miles");
			if (i % 2 > 0)
				h.setStylePrimaryName("resBody");
			else
				h.setStylePrimaryName("resBody1");
			
			final GMarker m = new GMarker(new GLatLng(new Double(doc.get("lat").toString()).doubleValue(),
										new Double(doc.get("lng").toString()).doubleValue()));
			
			map.gmap.addOverlay(m);

			map.gmem.addOnClickListener(m, new GMarkerEventClickListener(){

				public void onClick(GMarker marker) {
					marker.openInfoWindowHtml(cname);
					
				}

				public void onDblClick(GMarker marker) {
					marker.openInfoWindowHtml(cname);
					
				}
				
			});
			
			r.addMouseListener(new MouseListener (){

				public void onMouseDown(Widget sender, int x, int y) {
					
				}

				public void onMouseEnter(Widget sender) {
					enableDragSearch = false;
					m.openInfoWindowHtml(cname);
					
				}

				public void onMouseLeave(Widget sender) {
					enableDragSearch = true;
				}

				public void onMouseMove(Widget sender, int x, int y) {
					
				}

				public void onMouseUp(Widget sender, int x, int y) {
					
				}
				
			});
			
			r.addClickListener(new ClickListener() {

				public void onClick(Widget sender) {
				
					m.openInfoWindowHtml(cname);
				}
				
			});
			searchResults.add(r);
			searchResults.add(h);
		}
		results.clear();
		results.add(searchResults);
	}
	
	

	public void setResultsPanel(Panel results) {
		this.results = results;
		
	}
}
