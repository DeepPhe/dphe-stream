package org.healthnlp.deepphe.summary.attribute.util;

import java.util.Collection;
import java.util.List;


final public class AddFeatureUtil {

   private AddFeatureUtil() {}

   static public void addStandardFeatures( final List<Integer> features,
                                           final Collection<?> standard,
                                           final Collection<?> ... denominators ) {
      final int[] sizes = new int[ denominators.length ];
      for ( int i=0; i< denominators.length; i++ ) {
         sizes[ i ] = denominators[ i ].size();
      }
      addStandardFeatures( features, standard.size(), sizes );
   }

   static public void addStandardFeatures( final List<Integer> features,
                                           final int standard,
                                           final int ... denominators ) {
      addDoubleFeature( features, Math.log( 1 + standard ) );
      addDivisionFeature( features, 1, standard );
      for ( int d : denominators ) {
         addDivisionFeature( features, standard, d );
      }
   }

   static public void addCollectionFeatures( final List<Integer> features,
                                             final Collection<?> ... collections ) {
      for ( Collection<?> collection : collections ) {
         addDoubleFeature( features, Math.log( 1 + collection.size() ) );
         addDivisionFeature( features, 1, collection.size() );
      }
   }

   static public void addIntFeatures( final List<Integer> features,
                                      final int ... values ) {
      for ( int value : values ) {
         addDoubleFeature( features, Math.log( 1 + value ) );
         addDivisionFeature( features, 1, value );
      }
   }

   static public void addBooleanFeatures( final List<Integer> features, final boolean ... values ) {
      for ( boolean value : values ) {
         features.add( value ? 0 : 10 );
      }
   }

   static public void addIntFeature( final List<Integer> features,
                                     final int value ) {
      features.add( absolute0to10( value ) );
   }

   static public void addDoubleFeature( final List<Integer> features,
                                         final double value ) {
      features.add( absolute0to10( value ) );
   }

   static public void addDivisionFeature( final List<Integer> features,
                                          final int numerator,
                                          final int denominator ) {
      features.add( divisionInt0to10( numerator, denominator ) );
   }

   static public void addDoubleDivisionFeature( final List<Integer> features,
                                                final double numerator,
                                                final double denominator ) {
      features.add( divisionDouble0to10( numerator, denominator ) );
   }

   static public int absolute0to10( final double value ) {
      return Math.min( 10, (int)Math.ceil( value ) );
   }

   static public int divisionInt0to10( final int numerator,
                                       final int denominator ) {
      return divisionDouble0to10( (double)numerator, (double)denominator );
   }

   static public int divisionDouble0to10( final double numerator,
                                          final double denominator ) {
      final double ratio = numerator / denominator;
      return Math.min( 10, (int)Math.ceil( ratio*10d ) );
   }


}