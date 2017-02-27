// IBridgeInterface.aidl
package io.branch.search;
import io.branch.indexing.BranchUniversalObject;
import io.branch.search.BranchSearchContent;
import io.branch.search.IBranchSearchCallback;

interface IBranchSearchServiceInterface {
  void addToIndex (in BranchUniversalObject contentBUO, in String packageName, String contentUrl);
  void deleteFromIndex(in BranchUniversalObject contentBUO, in String packageName);
  void deleteAllSearchableContent (in String packageName);
  void searchContent(String keyword, int offset, int limit, String packageName);
  void registerCallback (in IBranchSearchCallback callback, String packageName);
  void addUserInteraction(in BranchUniversalObject contentBUO, in String packageName, String userAction, String contentUrl);
  void getTopRecommendedApps(int count, boolean skipSystemApps, String packageaName);
  void getTopRecommendedContents(int count, String packageaName);
  void searchInApp(String packageName, String searchKeyword);
}
