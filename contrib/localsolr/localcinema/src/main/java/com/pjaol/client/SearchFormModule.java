package com.pjaol.client;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FormHandler;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormSubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormSubmitEvent;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class SearchFormModule {

	final FormPanel form = new FormPanel();
	TextBox query = new TextBox();
	TextBox address = new TextBox();
	VerticalPanel panel = new VerticalPanel();
	private MapModule map;
	private SearchHandler searchHandler;
	private Panel results;
	
	public SearchFormModule (MapModule map){
		this.map = map;
		searchHandler = new SearchHandler(map, this);
		
	}
	
	public void setResults(Panel results){
		this.results = results;
		searchHandler.setResultsPanel(results);
	}
	
	public FormPanel getSearchForm() {
		query.setName("query");
		query.setText("*:*");
		
		address.setName("address");
		address.setText("NY, NY");
		
		form.setWidget(panel);
		
		panel.add(query);
		panel.add(address);
		
		panel.add(new Button("Search", new ClickListener() {
			public void onClick(Widget sender) {
				form.submit();
				
			}
		}));
		
		
		form.addFormHandler(new FormHandler() {
			public void onSubmitComplete(FormSubmitCompleteEvent event) {
				Window.alert("------"+event.getResults());
			}
			public void onSubmit(FormSubmitEvent event) {
				//Window.alert(query.getText() +" - "+ address.getText());
				searchHandler.search();
				event.setCancelled(true);
			}
		});
		
		return form;
	}
}
