package org.healthnlp.deepphe.summary.attribute.infostore;

public interface CodeInfoStore {

   void init( UriInfoStore uriInfoStore );

   String getBestCode();

}
