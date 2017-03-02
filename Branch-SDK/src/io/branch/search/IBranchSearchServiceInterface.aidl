// IBridgeInterface.aidl
package io.branch.search;
import io.branch.indexing.BranchUniversalObject;
import io.branch.search.BranchSearchResult;
import io.branch.search.IBranchSearchCallback;
import io.branch.search.SearchBuilder;
import io.branch.search.RecommendationBuilder;

interface IBranchSearchServiceInterface {
  void addToIndex (in BranchUniversalObject contentBUO, in String packageName, String contentUrl);
  void deleteFromIndex(in BranchUniversalObject contentBUO, in String packageName);
  void deleteAllSearchableContent (in String packageName);
  void searchContent(String packageName, in SearchBuilder searchBuilder);
  void registerCallback (in IBranchSearchCallback callback, String packageName);
  void addUserInteraction(in BranchUniversalObject contentBUO, in String packageName, String userAction, String contentUrl);
  void getRecommendations(String packageName, in RecommendationBuilder recommendationBuilder);
}
