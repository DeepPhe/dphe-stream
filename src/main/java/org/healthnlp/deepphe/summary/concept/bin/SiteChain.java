package org.healthnlp.deepphe.summary.concept.bin;

import org.healthnlp.deepphe.neo4j.node.Mention;

import java.util.*;

/**
 * @author SPF , chip-nlp
 * @since {6/9/2021}
 */
final public class SiteChain {

   private boolean _valid;
   private String _headUri;
   private final Map<String,Set<Mention>> _siteUriSites = new HashMap<>();

   SiteChain( final String headUri, final Map<String, Set<Mention>> siteUriSites ) {
      _headUri = headUri;
      _siteUriSites.putAll( siteUriSites );
      _valid = true;
   }

   boolean isValid() {
      return _valid;
   }

   void invalidate() {
      _valid = false;
   }

   String getHeadUri() {
      return _headUri;
   }

   Collection<String> getChainUris() {
      return _siteUriSites.keySet();
   }

   Map<String,Set<Mention>> getUriSites() {
      return _siteUriSites;
   }

   long scoreSiteByUrisMatch( final SiteChain otherChain ) {
      return scoreSiteByUrisMatch( otherChain.getChainUris() );
   }

   long scoreSiteByUrisMatch( final Collection<String> siteUris ) {
      return siteUris.stream()
                         .map( this::scoreSiteUriMatch )
                         .mapToLong( l -> l )
                         .sum() / _siteUriSites.size();
   }

   long scoreSiteUriMatch( final String siteUri ) {
      final Collection<Mention> matches = _siteUriSites.get( siteUri );
      if ( matches == null ) {
         return 0;
      }
      long score = matches.size();
      if ( siteUri.equals( _headUri ) ) {
         score *= 10;
      }
      return score;
   }

   long scoreSiteRootsMatch( final SiteChain otherChain, final Map<String,Collection<String>> allUriRoots ) {
      return otherChain.getChainUris().stream()
                       .map( u -> scoreSiteRootsMatch( u, allUriRoots ) )
                       .mapToLong( l -> l )
                       .sum();
   }

   long scoreSiteRootsMatch( final String siteUri, final Map<String,Collection<String>> allUriRoots ) {
      long score = 0;
      for ( Map.Entry<String,Set<Mention>> uriSites : _siteUriSites.entrySet() ) {
         final Collection<String> roots = allUriRoots.get( uriSites.getKey() );
         if ( roots.contains( siteUri ) ) {
            score += uriSites.getValue().size();
            if (  uriSites.getKey().equals( _headUri ) ) {
               score *= 10;
            }
         }
      }
      return score;
   }


   void copyInto( final SiteChain otherChain ) {
      _headUri = getBestHeadUriChain( this, otherChain )._headUri;
      otherChain._siteUriSites.forEach( (k,v) -> _siteUriSites.computeIfAbsent( k, s -> new HashSet<>() )
                                                              .addAll( v ) );
   }


   // This should be faster than trying to split a UriUtil.getAssociatedUri
   static private SiteChain getBestHeadUriChain( final SiteChain chain1, final SiteChain chain2 ) {
      if ( chain1._headUri.equals( chain2._headUri ) ) {
         return chain1;
      }
      final int headDiff = chain1._siteUriSites.get( chain1._headUri ).size()
                           - chain2._siteUriSites.get( chain2._headUri ).size();
      if ( headDiff > 0 ) {
         return chain1;
      } else if ( headDiff < 0 ) {
         return chain2;
      }
      final int allDiff = chain1._siteUriSites.values().stream().mapToInt( Collection::size ).sum()
                          - chain2._siteUriSites.values().stream().mapToInt( Collection::size ).sum();
      if ( allDiff > 0 ) {
         return chain1;
      } else if ( allDiff < 0 ) {
         return chain2;
      }
      final int uriDiff = chain1._siteUriSites.size() - chain2._siteUriSites.size();
      if ( uriDiff > 0 ) {
         return chain1;
      } else if ( uriDiff < 0 ) {
         return chain2;
      }
      return chain1;
   }


   public String toString() {
      return "SiteChain " + _headUri + " : " + String.join( ";", getChainUris() ) + "\n";
   }


}
