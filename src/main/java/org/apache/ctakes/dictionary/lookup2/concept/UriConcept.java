package org.apache.ctakes.dictionary.lookup2.concept;

import jdk.nashorn.internal.ir.annotations.Immutable;
import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.healthnlp.deepphe.neo4j.constant.Neo4jConstants.DPHE_CODING_SCHEME;

/**
 * Uses URI to create semantic group.
 * @author SPF , chip-nlp
 * @since {3/11/2023}
 */
@Immutable
final public class UriConcept implements Concept {

   private final Concept _delegate;

   private final Collection<Integer> _ctakesSemantics;


   public UriConcept(final Concept delegate ) {
      _delegate = delegate;
      final Collection<String> codes = getCodes( DPHE_CODING_SCHEME );
      String uri = UriConstants.UNKNOWN;
      if ( codes != null && !codes.isEmpty() ) {
         uri = new ArrayList<>( codes ).get( 0 );
      }
      final SemanticTui semanticTui = UriInfoCache.getInstance().getSemanticTui( uri );
      _ctakesSemantics = Collections.singletonList( semanticTui.getGroupCode() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getCui() {
      return _delegate.getCui();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getPreferredText() {
      return _delegate.getPreferredText();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<String> getCodeNames() {
      return _delegate.getCodeNames();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<String> getCodes( final String codeType ) {
      return _delegate.getCodes( codeType );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<Integer> getCtakesSemantics() {
      return _ctakesSemantics;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isEmpty() {
      return _delegate.isEmpty();
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals( final Object value ) {
      return _delegate.equals( value );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int hashCode() {
      return _delegate.hashCode();
   }


}
