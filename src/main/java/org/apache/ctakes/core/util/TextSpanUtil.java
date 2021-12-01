package org.apache.ctakes.core.util;

import org.apache.log4j.Logger;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.*;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @since {11/19/2021}
 */
final public class TextSpanUtil {

   static private final Logger LOGGER = Logger.getLogger( "TextSpanUtil" );

   private TextSpanUtil() {}

   static public Collection<Pair<Integer>> getAvailableSpans( final int windowOffset,
                                                               final int windowLength,
                                                               final Collection<? extends Annotation> unavailableCoverings ) {
      final int begin = 0;
      if ( unavailableCoverings.isEmpty() ) {
         return Collections.singletonList( new Pair<>( begin, windowLength ) );
      }
      final List<Pair<Integer>> unavailableSpans
            = unavailableCoverings.stream()
                       .filter( t -> t.getEnd() > windowOffset + begin && t.getBegin() < windowOffset + windowLength )
                       .sorted( Comparator.comparingInt( Annotation::getBegin )
                                          .thenComparing( Annotation::getEnd ) )
                       .map( t -> new Pair<>( t.getBegin()-windowOffset, t.getEnd()-windowOffset ) )
                       .collect( Collectors.toList() );
      return getAvailableSpans( windowLength, unavailableSpans );
   }


   static public Collection<Pair<Integer>> getAvailableSpans( final int windowLength,
                                                              final List<Pair<Integer>> unavailableSpans ) {
      final int begin = 0;
      if ( unavailableSpans.isEmpty() ) {
         return Collections.singletonList( new Pair<>( begin, windowLength ) );
      }
      final Collection<Pair<Integer>> availableSpans = new ArrayList<>();
      int previousEnd = begin;
      for ( final Pair<Integer> span : unavailableSpans ) {
         if ( previousEnd < span.getValue1() ) {
            // previous end is more than one line previous to the new span's begin.  Add span between as available.
            availableSpans.add( new Pair<>( previousEnd, span.getValue1() ) );
         }
         previousEnd = span.getValue2();
      }
      if ( previousEnd < windowLength ) {
         // previous end is more than one line previous to the new span's begin.  Add span between as available.
         availableSpans.add( new Pair<>( previousEnd, windowLength ) );
      }
//      LOGGER.info( "Spans still available: " + availableSpans.size() + " "
//                   + availableSpans.stream()
//                                   .map( p -> p.getValue1() +"," + p.getValue2() )
//                                   .collect( Collectors.joining( "  " ) ) );
      return availableSpans;
   }


   static public Collection<Pair<Integer>> getAvailableSpans( final Annotation window,
                                                              final List<Pair<Integer>> unavailableSpans ) {
      if ( unavailableSpans.isEmpty() ) {
         return Collections.singletonList( new Pair<>( window.getBegin(), window.getEnd() ) );
      }
      final Collection<Pair<Integer>> windowUnavailables
            = unavailableSpans.stream()
                            .filter( t -> t.getValue1() >= window.getBegin() && t.getValue2() <= window.getEnd() )
                            .collect( Collectors.toList() );
      if ( windowUnavailables.isEmpty() ) {
         return Collections.singletonList( new Pair<>( window.getBegin(), window.getEnd() ) );
      }
      final Collection<Pair<Integer>> availableSpans = new ArrayList<>();
      int previousEnd = window.getBegin();
      for ( final Pair<Integer> span : windowUnavailables ) {
         if ( previousEnd < span.getValue1() ) {
            // previous end is more than one line previous to the new span's begin.  Add span between as available.
            availableSpans.add( new Pair<>( previousEnd, span.getValue1() ) );
         }
         previousEnd = span.getValue2();
      }
      if ( previousEnd < window.getEnd() ) {
         // previous end is more than one line previous to the new span's begin.  Add span between as available.
         availableSpans.add( new Pair<>( previousEnd, window.getEnd() ) );
      }
      return availableSpans;
   }



   static public boolean isAnnotationCovered( final List<Pair<Integer>> unavailableSpans,
                                              final Annotation annotation ) {
      if ( unavailableSpans.isEmpty() ) {
         return false;
      }
      final int begin = annotation.getBegin();
      final int end = annotation.getEnd();
      return unavailableSpans.stream()
                             .anyMatch( s -> s.getValue1() <= begin && end <= s.getValue2() );
   }


   static public boolean isValidSpan( final Pair<Integer> span ) {
      return span.getValue1() >= 0 && span.getValue2() >= span.getValue1();
   }

   static public boolean isValidSpan( final Annotation annotation ) {
      return annotation.getBegin() >= 0 && annotation.getEnd() >= annotation.getBegin();
   }


   /**
    * Sorts by first offset, LONGER bounds first:
    *   |=============|
    *      |========|
    *      |====|
    *        |==============|
    *                |==============|
    *                     |========|
    */
   static public final class CoveringSpanSorter implements Comparator<Pair<Integer>> {
      public int compare( final Pair<Integer> p1, final Pair<Integer> p2 ) {
         final int start = p1.getValue1() - p2.getValue1();
         if ( start != 0 ) {
            return start;
         }
         return p2.getValue2() - p1.getValue2();
      }
   }


   /**
    * Sorts by first offset, LONGER bounds first:
    *   |=============|
    *      |========|
    *      |====|
    *        |==============|
    *                |==============|
    *                     |========|
    */
   static public final class CoveringAnnotationSorter implements Comparator<Annotation> {
      public int compare( final Annotation a1, final Annotation a2 ) {
         final int start = a1.getBegin() - a2.getBegin();
         if ( start != 0 ) {
            return start;
         }
         return a2.getEnd() - a1.getEnd();
      }
   }


   /**
    * Sorts by span size, smallest to longest
    */
   static public final class SpanSizeSorter implements Comparator<Pair<Integer>> {
      public int compare( final Pair<Integer> p1, final Pair<Integer> p2 ) {
         return (p1.getValue2()-p1.getValue1()) - (p2.getValue2()-p2.getValue1());
      }
   }


   /**
    * Sorts by span size, smallest to longest
    */
   static public final class AnnotationSizeSorter implements Comparator<Annotation> {
      public int compare( final Annotation a1, final Annotation a2 ) {
         return (a1.getEnd()-a1.getBegin()) - (a2.getEnd()-a2.getBegin());
      }
   }


}
