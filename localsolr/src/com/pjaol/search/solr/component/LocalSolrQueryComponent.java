package com.pjaol.search.solr.component;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Sort;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.QueryComponent;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryResponse;
import org.apache.solr.search.*;
import org.apache.solr.update.DocumentBuilder;
import org.apache.solr.util.SolrPluginUtils;

import com.pjaol.search.geo.utils.DistanceQuery;
import com.pjaol.search.geo.utils.DistanceSortSource;
import com.pjaol.search.geo.utils.DistanceUtils;
import com.pjaol.search.solr.LocalSolrRequestHandler;

/**
 * {@link LocalSolrRequestHandler}
 *
 * @author pjaol
 * @see LocalSolrRequestHandler
 */
public class LocalSolrQueryComponent extends QueryComponent {


  private String DistanceQuery = "DistanceQuery";

  private String DistanceCache = "distanceCache";

  public static final String COMPONENT_NAME = "localsolr";


  private Logger log = Logger.getLogger(getClass().getName());
  private String latField = "lat";
  private String lngField = "lng";

  public LocalSolrQueryComponent() {
    super();
  }

  public LocalSolrQueryComponent(String lat, String lng) {
    super();

    if (lat != null && lng != null) {
      log.info("Setting latField to " + lat + " setting lngField to "
        + lng);
      latField = lat;
      lngField = lng;
    }
  }

  @Override
  public void init(NamedList initArgs) {
    super.init(initArgs);
    String lat = (String) initArgs.get("latField");
    String lng = (String) initArgs.get("lngField");

    if (lat != null && lng != null) {
      log.info("Setting latField to " + lat + " setting lngField to "
        + lng);
      latField = lat;
      lngField = lng;
    }
  }

  public void prepare(ResponseBuilder rb) throws IOException {
    super.prepare(rb);
    SolrQueryRequest req = rb.req;
    SolrQueryResponse rsp = rb.rsp;
    SolrParams params = req.getParams();

    String lat = params.get("lat");
    String lng = params.get("long");

    String radius = params.get("radius");

    DistanceSortSource dsort = null;

    DistanceQuery dq = null;

    if (lat != null && lng != null && radius != null) {

      double dlat = new Double(lat).doubleValue();
      double dlng = new Double(lng).doubleValue();
      double dradius = new Double(radius).doubleValue();

      // TODO pull latitude /longitude from configuration
      dq = new DistanceQuery(dlat, dlng, dradius, latField, lngField,
        true);

      dsort = new DistanceSortSource(dq.distanceFilter);
    }

    req.getContext().put(DistanceQuery, dq);
  }

  @Override
  public void process(ResponseBuilder rb) throws IOException {
    SolrQueryRequest req = rb.req;
    SolrQueryResponse rsp = rb.rsp;
    SolrIndexSearcher searcher = req.getSearcher();
    SolrParams params = req.getParams();

    String lat = params.get("lat");
    String lng = params.get("long");

    SolrCache distanceCache = searcher.getCache(DistanceCache);

    DocSet f = null;
    DistanceQuery dq = (DistanceQuery) req.getContext().get(DistanceQuery);

    boolean cachedDistances = false;

    Filter optimizedDistanceFilter = dq.getFilter(rb.getQuery());

    Map<Integer, Double> distances = null;

    if (distanceCache != null) {

      // Does this region have it's geography cached?
      distances = (Map<Integer, Double>) distanceCache
        .get(dq.distanceFilter);
      if (distances != null) {
        dq.distanceFilter.setDistances(distances);
        cachedDistances = true;
      } else {
        // try and get cache for a query based geography filter.
        distances = (Map<Integer, Double>) distanceCache
          .get(optimizedDistanceFilter);
        if (distances != null) {
          dq.distanceFilter.setDistances(distances);
          cachedDistances = true;
        }
      }

    }

    rb.getFilters().add(dq.getQuery());

    super.process(rb);

    if (!cachedDistances) {
      if (distanceCache != null) {
        distanceCache.put(optimizedDistanceFilter, dq.distanceFilter
          .getDistances());
      }
    }

//    Sort sort = null;
//    if (rb.getSortSpec() != null) {
//      sort = rb.getSortSpec().getSort();
//    }

//    if (rb.isNeedDocSet()) {
//
//      if (!cachedDistances) {
//        // use a standard query
//        log.fine("Standard query...");
//
//        rb.setResults(searcher
//            .getDocListAndSet(rb.getQuery(), f, sort,
//            params.getInt(CommonParams.START, 0),
//            params.getInt(CommonParams.ROWS, 10),
//            rb.getFieldFlags()));
//      } else {
//        // use a cached query
//        log.fine("Cached query....");
//        rb.setResults(searcher
//          .getDocListAndSet(rb.getQuery(), dq.getQuery(),
//          sort, params.getInt(CommonParams.START, 0),
//          params.getInt(CommonParams.ROWS, 10), rb
//          .getFieldFlags()));
//      }
//
//    } else {
//
//      log.fine("DocList query....");
//      DocListAndSet results = new DocListAndSet();
//      if (!cachedDistances) {
//        log.fine("Using reqular...");
//
//        results.docList = searcher.getDocList(rb.getQuery(), f,
//          sort, params.getInt(CommonParams.START, 0), params
//          .getInt(CommonParams.ROWS, 10));
//      } else {
//        log.fine("Using cached.....");
//        results.docList = searcher
//          .getDocList(rb.getQuery(), rb.getFilters(),
//            sort, params.getInt(CommonParams.START, 0),
//            params.getInt(CommonParams.ROWS, 10), rb
//            .getFieldFlags());
//      }
//      rb.setResults(results);
//
//    }

    if (distances == null)
      distances = dq.distanceFilter.getDistances();

    // pre-fetch returned documents
    SolrPluginUtils.optimizePreFetchDocs(rb.getResults().docList,
      rb.getQuery(), req, rsp);


    SolrDocumentList sdoclist = mergeResultsDistances(
      rb.getResults().docList, distances, searcher, rsp
      .getReturnFields(), new Double(lat).doubleValue(),
      new Double(lng).doubleValue());

    log.finer("Adding SolrDocumentList " + sdoclist.size());

    //remove the result set from the query component, replacing it with our filtered set.
    NamedList values = rsp.getValues();
    int index = values.indexOf("response", 0);
    values.remove(index);
      
    rsp.add("response", sdoclist);

    if (dq.distanceFilter != null) {
      //sort = null;
      distances = null;

    }

  }

  private SolrDocumentList mergeResultsDistances(DocList docs, Map distances,
                                                 SolrIndexSearcher searcher, Set<String> fields, double latitude,
                                                 double longitude) {
    SolrDocumentList sdoclist = new SolrDocumentList();
    sdoclist.setNumFound(docs.matches());
    sdoclist.setMaxScore(docs.maxScore());
    sdoclist.setStart(docs.offset());

    DocIterator dit = docs.iterator();

    while (dit.hasNext()) {
      int docid = dit.nextDoc();
      try {
        SolrDocument sd = luceneDocToSolrDoc(docid, searcher, fields);
        if (distances != null) {

          Object cached_distance = distances.get(docid);
          if(cached_distance != null) {
            sd.addField("geo_distance", new String(cached_distance.toString()).toString());
          }

        } else {
          if (sd.getFieldNames().contains(latField)){
            double docLat = (Double) sd.getFieldValue(latField);
            double docLong = (Double) sd.getFieldValue(lngField);
            double distance = DistanceUtils.getDistanceMi(docLat,
              docLong, latitude, longitude);

            sd.addField("geo_distance", distance);
          }
        }

        // this may be removed if XMLWriter gets patched to
        // include score from doc iterator in solrdoclist
        if (docs.hasScores()) {
          sd.addField("score", dit.score());
        } else {
          sd.addField("score", 0.0f);
        }

        if(sd.getFieldNames().contains("geo_distance")){
          sdoclist.add(sd);
        }
      } catch (IOException e) {
        // TODO possible slip or should we fail?
        e.printStackTrace();
      }

    }

    return sdoclist;
  }

  public SolrDocument luceneDocToSolrDoc(int docid,
                                         SolrIndexSearcher searcher, Set fields) throws IOException {
    Document luceneDoc = searcher.doc(docid, fields);

    SolrDocument sdoc = new SolrDocument();
    DocumentBuilder db = new DocumentBuilder(searcher.getSchema());
    sdoc = db.loadStoredFields(sdoc, luceneDoc);
    return sdoc;
  }

  /*
    * Solr InfoBean
    */

  @Override
  public String getDescription() {

    return "LocalSolrQueryComponent2: $File: $";
  }

  @Override
  public String getSource() {

    return "$File: $";
  }

  @Override
  public String getSourceId() {

    return "$Id: $";
  }

  @Override
  public String getVersion() {

    return "$Version: $";
  }
}
