package org.apache.ctakes.dictionary.lookup2.concept;

import org.apache.ctakes.dictionary.lookup2.util.CuiCodeUtil;
import org.apache.uima.UimaContext;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/11/2023}
 */
public class UriConceptFactory implements ConceptFactory {

   final private ConceptFactory _delegateConceptFactory;

   public UriConceptFactory( final String name, final UimaContext uimaContext, final Properties properties )
         throws SQLException {
      _delegateConceptFactory = new JdbcConceptFactory( name, uimaContext, properties );
      // Seed the CuiCodeUtil with a CL nci cui.  After this nci cuis in the dictionary should be assigned CL.
      CuiCodeUtil.getInstance().getCuiCode( "CL000001" );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public String getName() {
      return _delegateConceptFactory.getName();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Concept createConcept( final Long cuiCode ) {
      final Concept ctakesConcept = _delegateConceptFactory.createConcept( cuiCode );
      return new UriConcept( ctakesConcept );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Map<Long, Concept> createConcepts( final Collection<Long> cuiCodes ) {
      return cuiCodes.stream()
                     .collect( Collectors.toMap( Function.identity(), this::createConcept ) );
   }


}
