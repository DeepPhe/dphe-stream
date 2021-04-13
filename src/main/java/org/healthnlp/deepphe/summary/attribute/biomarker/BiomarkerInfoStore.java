package org.healthnlp.deepphe.summary.attribute.biomarker;

import org.healthnlp.deepphe.summary.attribute.infostore.AttributeInfoStore;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {4/9/2021}
 */
public class BiomarkerInfoStore extends AttributeInfoStore<BiomarkerUriInfoVisitor, BiomarkerCodeInfoStore> {

   public BiomarkerInfoStore( final ConceptAggregate neoplasm,
                              final Supplier<BiomarkerUriInfoVisitor> uriVisitorCreator,
                              final Supplier<BiomarkerCodeInfoStore> codeInfoStoreCreator,
                              final Map<String,String> dependencies ) {
      this( Collections.singletonList( neoplasm ), uriVisitorCreator, codeInfoStoreCreator, dependencies );
   }

   public BiomarkerInfoStore( final Collection<ConceptAggregate> neoplasms,
                              final Supplier<BiomarkerUriInfoVisitor> uriVisitorCreator,
                              final Supplier<BiomarkerCodeInfoStore> codeInfoStoreCreator,
                              final Map<String,String> dependencies ) {
      super( neoplasms, uriVisitorCreator, codeInfoStoreCreator, dependencies );
   }

   /**
    *
    * @return The covered text of biomarker values in the documents.  Ugly, but at this point as good as it gets.
    */
   @Override
   public String getBestCode() {
      return _concepts.stream()
                      .map( ConceptAggregate::getCoveredText )
                      .collect( Collectors.joining( " ; " ) );
   }


}
