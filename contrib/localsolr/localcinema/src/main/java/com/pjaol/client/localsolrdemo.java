package com.pjaol.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class localsolrdemo implements EntryPoint {

	
	private DockPanel main = new DockPanel();
	private HTML historyFrame = new HTML();
	private MapModule map = new MapModule();
	private SearchFormModule searchForm = new SearchFormModule(map);
	
	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {

		main.add(historyFrame, DockPanel.SOUTH);
	    
	    RootPanel.get().add(main);
	    Panel results = RootPanel.get("results");
	    searchForm.setResults(results);
	    RootPanel.get("map").add(map.getMap());
	    RootPanel.get("search").add(searchForm.getSearchForm());
	    
	}
}
