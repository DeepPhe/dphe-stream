package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.regex.RegexUtil;
import org.apache.ctakes.typesystem.type.textspan.FormattedListEntry;
import org.apache.ctakes.typesystem.type.textspan.NormalizableAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author SPF , chip-nlp
 * @since {10/26/2021}
 */
final public class ListEntryFinder {
   // TODO Replace ctakes ListAnnotator in core.ae with this and the rest of the formatted list stuff.
   // TODO Deprecate the old ListAnnotator stuff.

   static private final Logger LOGGER = Logger.getLogger( "ListEntryFinder" );

   static private final String INDEX_GROUP = "Index";
   static private final String NAME_GROUP = "Name";
   static private final String VALUE_GROUP = "Value";
   static private final String DETAILS_GROUP = "Details";

   static public List<FormattedListEntry> createListEntries( final JCas jCas,
                                                              final int listOffset,
                                                              final String listText,
                                                              final Pattern entryPattern ) {
      final List<FormattedListEntry> entries = new ArrayList<>();
      final Matcher matcher = entryPattern.matcher( listText );
      while ( matcher.find() ) {
         final FormattedListEntry entry = new FormattedListEntry( jCas,
                                                                  listOffset + matcher.start(),
                                                                  listOffset + matcher.end() );
         entry.setIndex( createNormalizable( jCas, listOffset, matcher, INDEX_GROUP ) );
         entry.setName( createNormalizable( jCas, listOffset, matcher, NAME_GROUP ) );
         entry.setValue( createNormalizable( jCas, listOffset, matcher, VALUE_GROUP ) );
         entry.setDetails( createNormalizable( jCas, listOffset, matcher, DETAILS_GROUP ) );
         entry.addToIndexes();
         entries.add( entry );
      }
      return entries;
   }

   static private NormalizableAnnotation createNormalizable( final JCas jCas,
                                                        final int spanOffset,
                                                        final Matcher matcher,
                                                             final String groupName ) {
      final Pair<Integer> span = RegexUtil.getGroupSpan( matcher, groupName );
      if ( !RegexUtil.isValidSpan( span ) ) {
         return null;
      }
      final NormalizableAnnotation name = new NormalizableAnnotation( jCas,
                                                                        spanOffset + matcher.start(),
                                                                        spanOffset + matcher.end() );
      name.addToIndexes();
      return name;
   }


}

