package org.healthnlp.deepphe.nlp.uri;

import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.neo4j.util.Neo4jRelationUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author SPF , chip-nlp
 * @since {3/9/2023}
 */
public enum UriInfoCache {
   INSTANCE;

   static public UriInfoCache getInstance() {
      return INSTANCE;
   }

   static private final Object LOCK = new Object();
   // 1 Hour
//   static private final long TIMEOUT = 1000 * 60 * 60;
   // 4 minutes
   static private final long TIMEOUT = 1000 * 60 * 4;

   static private final long PERIOD = TIMEOUT / 4;
   static private final long START = TIMEOUT + PERIOD;

   static public final String PRIMARY_SITE = RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE;
   static public final String ASSOCIATED_SITE = RelationConstants.DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE;


   private final Map<String, Long> _timeMap = new ConcurrentHashMap<>();
   private final Map<String, SemanticTui> _uriSemanticMap = new ConcurrentHashMap<>();
   private final Map<String, Collection<String>> _uriBranchMap = new ConcurrentHashMap<>();
   private final Map<String, Integer> _uriBranchSizeMap = new ConcurrentHashMap<>();
   private final Map<String, Collection<String>> _uriRootMap = new ConcurrentHashMap<>();
   private final Map<String, Integer> _uriLevelMap = new ConcurrentHashMap<>();
   private final Map<String, Double> _uriMidLevelMap = new ConcurrentHashMap<>();
   private final Map<String, UriNode> _uriNodeMap = new ConcurrentHashMap<>();
   private final ScheduledExecutorService _cacheCleaner;

   UriInfoCache() {
      _cacheCleaner = Executors.newScheduledThreadPool( 1 );
      _cacheCleaner.scheduleAtFixedRate( new CacheCleaner(), START, PERIOD, TimeUnit.MILLISECONDS );
   }

   public void close() {
      _cacheCleaner.shutdown();
   }


   /**
    *
    * @param uris all unique URIs for all annotations in the document.
    */
   public Map<String,UriNode> createDocUriNodeMap( final Collection<String> uris ) {
      final Map<String,UriNode> uriNodes = new HashMap<>( uris.size() );
      for ( String uri : uris ) {
         getUriBranch( uri );
         getUriBranchSize( uri );
         getUriRoots( uri );
         getUriLevel( uri );
         getUriMidLevel( uri );
         uriNodes.put( uri, getUriNode( uri ) );
      }
      return uriNodes;
   }

   static private final Collection<String> LATERALITY_URIS = Arrays.asList( "Left", "Right", "Bilateral",
                                                                         "Unspecified_Laterality",
                                                                         "Unilateral", "Unilateral_Left",
                                                                         "Unilateral_Right" );


   /**
    *
    * @param uri -
    * @return -
    */
   public SemanticTui getSemanticTui( final String uri ) {
      final long millis = System.currentTimeMillis();
      synchronized ( LOCK ) {
         final SemanticTui cachedSemantic = _uriSemanticMap.get( uri );
         if ( cachedSemantic != null ) {
            _timeMap.put( uri, millis );
            return cachedSemantic;
         }
         final GraphDatabaseService graphDb = EmbeddedConnection.getInstance().getGraph();
         // Finding by default
         SemanticTui semantic = SemanticTui.T033;
         if ( UriConstants.getMassNeoplasmUris( graphDb ).contains( uri ) ) {
            // Neoplastic Process
            semantic = SemanticTui.T191;
         } else if ( UriConstants.getPositiveValueUris( graphDb ).contains( uri )
                     || UriConstants.getRegaultValueUris( graphDb ).contains( uri )
                     || UriConstants.getNormalValueUris( graphDb ).contains( uri )
                     || UriConstants.getStableValueUris( graphDb ).contains( uri )
                     || UriConstants.getHighValueUris( graphDb ).contains( uri ) ) {
            // Laboratory or Test Result
            return SemanticTui.T034;
         } else if ( LATERALITY_URIS.contains( uri )
                     || CustomUriRelations.getInstance().getQuadrantUris().contains( uri )
                     || getUriRoots( uri ).contains( UriConstants.CLOCKFACE ) ) {
            // Spatial Concept
            return SemanticTui.T082;
         } else if ( CustomUriRelations.getInstance().getStageUris().contains( uri )
                     || CustomUriRelations.getInstance().getGradeUris().contains( uri )
                     || CustomUriRelations.getInstance().getBehaviorUris().contains( uri )
                     || getUriBranch( "Generic_TNM" ).contains( uri )
                     || getUriBranch( "Pathologic_TNM" ).contains( uri ) ) {
            // Clinical Attribute
            return SemanticTui.T201;
         } else if ( UriConstants.getLocationUris( graphDb ).contains( uri ) ) {
            // Body Part, Organ, or Organ Component
            semantic = SemanticTui.T023;
         }
         _timeMap.put( uri, millis );
         _uriSemanticMap.put( uri, semantic );
         return semantic;
      }
   }

   /**
    *
    * @param uri -
    * @return All child URIs.  Does NOT include URI itself.
    */
   public Collection<String> getUriBranch( final String uri ) {
      final long millis = System.currentTimeMillis();
      synchronized ( LOCK ) {
         final Collection<String> cachedBranch = _uriBranchMap.get( uri );
         if ( cachedBranch != null ) {
            _timeMap.put( uri, millis );
            return cachedBranch;
         }
         final Collection<String> branch =  Neo4jOntologyConceptUtil.getBranchUris( uri );
         branch.remove( uri );
         _timeMap.put( uri, millis );
         _uriBranchMap.put( uri, branch );
         return branch;
      }
   }

   public int getUriBranchSize( final String uri ) {
      synchronized ( LOCK ) {
         final Integer cachedBranchSize = _uriBranchSizeMap.get( uri );
         if ( cachedBranchSize != null ) {
            return cachedBranchSize;
         }
         final int branchSize =  getUriBranch( uri ).size();
         _uriBranchSizeMap.put( uri, branchSize );
         return branchSize;
      }
   }

   /**
    *
    * @param uri -
    * @return All ancestor URIs.  DOES include URI itself.
    */
   public Collection<String> getUriRoots( final String uri ) {
      final long millis = System.currentTimeMillis();
      synchronized ( LOCK ) {
         final Collection<String> cachedRoots = _uriRootMap.get( uri );
         if ( cachedRoots != null ) {
            _timeMap.put( uri, millis );
            return cachedRoots;
         }
         final Collection<String> roots =  Neo4jOntologyConceptUtil.getRootUris( uri );
         roots.remove( "Thing" );
         roots.remove( "DeepPhe" );
         _timeMap.put( uri, millis );
         _uriRootMap.put( uri, roots );
         return roots;
      }
   }

   private int getUriLevel( final String uri ) {
      if ( uri.equals( "Thing" ) || uri.equals( "DeepPhe" ) ) {
         return 0;
      }
      synchronized ( LOCK ) {
         final Integer cachedLevel = _uriLevelMap.get( uri );
         if ( cachedLevel != null ) {
            return cachedLevel;
         }
         final int level = Neo4jOntologyConceptUtil.getClassLevel( uri );
         _uriLevelMap.put( uri, level );
         return level;
      }
   }


   private double getUriMidLevel( final String uri ) {
      if ( uri.equals( "Thing" ) || uri.equals( "DeepPhe" ) ) {
         return 0;
      }
      synchronized ( LOCK ) {
         final Double cachedLevel = _uriMidLevelMap.get( uri );
         if ( cachedLevel != null ) {
            return cachedLevel;
         }
         final int level = getUriLevel( uri );
         final int maxLevel = getUriRoots( uri ).stream()
                                             .mapToInt( UriInfoCache.this::getUriLevel )
                                             .max()
                                             .orElse( 0 ) + 1;
         _uriMidLevelMap.put( uri, (level+maxLevel) / 2d );
         return level;
      }
   }


   public UriNode getUriNode( final String uri ) {
      final long millis = System.currentTimeMillis();
      synchronized ( LOCK ) {
         final UriNode cachedNode = _uriNodeMap.get( uri );
         if ( cachedNode != null ) {
            _timeMap.put( uri, millis );
            return cachedNode;
         }
         // TODO - cache relations ?
         final GraphDatabaseService graphDb = EmbeddedConnection.getInstance().getGraph();
         final Map<String, Collection<String>> uriRelations = Neo4jRelationUtil.getRelatedClassUris( graphDb, uri );
         final Map<String,Collection<String>> siteRelations = new HashMap<>();
         final Map<String,Collection<String>> nonSiteRelations = new HashMap<>();
         for ( Map.Entry<String,Collection<String>> relation : uriRelations.entrySet() ) {
            if ( RelationConstants.isHasSiteRelation( relation.getKey() ) ) {
               String siteRelation = PRIMARY_SITE;
               if ( relation.getKey().toLowerCase().contains( "associated" ) ) {
                  siteRelation = ASSOCIATED_SITE;
               }
               siteRelations.computeIfAbsent( siteRelation, s -> new HashSet<>() )
                            .addAll( relation.getValue() );
            } else {
               nonSiteRelations.computeIfAbsent( relation.getKey(), s -> new HashSet<>() )
                               .addAll( relation.getValue() );
            }
         }
         nonSiteRelations.putAll( CustomUriRelations.getInstance().getNeoplasmRelations( uri, graphDb ) );
         final UriNode node = new UriNode( uri, nonSiteRelations, siteRelations );
         _timeMap.put( uri, millis );
         _uriNodeMap.put( uri, node );
         return node;
      }
   }



   public class UriNode {
      final private String _uri;
      final private double _uriMidLevel;
      final private Map<String, Collection<String>> _siteRelations = new HashMap<>();
      final private Map<String, Collection<String>> _nonSiteRelations = new HashMap<>();

      private UriNode( final String uri,
                        final Map<String, Collection<String>> nonSiteRelations,
                       final Map<String,Collection<String>> siteRelations ) {
         _uri = uri;
         _uriMidLevel = getUriMidLevel( uri );
         _nonSiteRelations.putAll( nonSiteRelations );
         _siteRelations.putAll( siteRelations );
      }

      public String getUri() {
         return _uri;
      }

      public boolean lacksRelations() {
         return _nonSiteRelations.isEmpty() && _siteRelations.isEmpty();
      }

      public Map<String, Double> getRelationScores( final String targetUri ) {
         final Map<String, Double> relationScores = new HashMap<>();
         final Collection<String> targetRoots = getUriRoots( targetUri );
         final double targetMidLevel = getUriMidLevel( targetUri );
         if ( getInstance().getSemanticTui( targetUri ) == SemanticTui.T023 ) {
            fillRelationScores( targetRoots, targetMidLevel, _siteRelations, relationScores );
         } else {
            fillRelationScores( targetRoots, targetMidLevel, _nonSiteRelations, relationScores );
            // Special case for quadrants (e.g. Nipple)
            if ( CustomUriRelations.getInstance().getQuadrantUris().contains( targetUri ) ) {
               fillRelationScores( targetRoots, targetMidLevel, _siteRelations, relationScores );
            }
         }
         return relationScores;
      }

      private void fillRelationScores( final Collection<String> targetRoots,
                         final double targetMidLevel,
                         final Map<String, Collection<String>> relationsMap,
                         final Map<String, Double> relationScores ) {
         for ( Map.Entry<String, Collection<String>> relatedUris : relationsMap.entrySet() ) {
            double maxScore = 0d;
            for ( String relatedUri : relatedUris.getValue() ) {
               if ( targetRoots.contains( relatedUri ) ) {
                  maxScore = Math.max( maxScore, getTargetUriScore( relatedUri, targetMidLevel ) );
               }
            }
            if ( maxScore > 10d ) {
               relationScores.put( relatedUris.getKey(), maxScore );
            }
         }
      }

      private double getTargetUriScore( final String relatedUri, final double targetMidLevel ) {
         final double relatedMidLevel = getUriMidLevel( relatedUri );
         //  If a relatedUri has few children then give it a bump in value.  e.g. laterality.
         final int branchSize = getUriBranchSize( relatedUri );
         final double bump = branchSize > 10 ? 0 : 20;
         // The most important thing is the level of the related uri.  The depth of the target is secondary.
         return Math.min( 100,
                          10d * _uriMidLevel
                          + 10d * relatedMidLevel
                          + bump
                          + 2d * ( targetMidLevel - relatedMidLevel ) );
      }
   }


   private final class CacheCleaner implements Runnable {
      public void run() {
         final long old = System.currentTimeMillis() - TIMEOUT;
         synchronized ( LOCK ) {
            final Collection<String> removals = new ArrayList<>();
            for ( Map.Entry<String, Long> timeEntry : _timeMap.entrySet() ) {
               if ( timeEntry.getValue() < old ) {
                  removals.add( timeEntry.getKey() );
               }
            }
            for ( String removal : removals ) {
               _timeMap.remove( removal );
               _uriBranchMap.remove( removal );
               _uriBranchSizeMap.remove( removal );
               _uriRootMap.remove( removal );
               _uriLevelMap.remove( removal );
               _uriMidLevelMap.remove( removal );
               _uriNodeMap.remove( removal );
            }
         }
      }
   }


}
