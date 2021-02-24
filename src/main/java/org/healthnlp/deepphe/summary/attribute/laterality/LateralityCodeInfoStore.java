package org.healthnlp.deepphe.summary.attribute.laterality;

import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoStore;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.Collection;

public class LateralityCodeInfoStore {

   public String _bestLateralityCode;


   protected void init( final Collection<ConceptAggregate> neoplasms,
                        final UriInfoStore uriInfoStore ) {
      _bestLateralityCode = "9";
   }



}
