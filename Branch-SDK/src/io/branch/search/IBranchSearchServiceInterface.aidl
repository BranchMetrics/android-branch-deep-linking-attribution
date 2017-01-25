// IBridgeInterface.aidl
package io.branch.search;
import io.branch.indexing.BranchUniversalObject;
import io.branch.search.BranchSearchContent;
import io.branch.search.IBranchSearchCallback;

interface IBranchSearchServiceInterface {
  void addToSharableContent (in BranchUniversalObject contentBUO, in String packageName, String contentUrl);
  void searchContent(String keyword, int offset, int limit, String packageName);
  void registerCallback (in IBranchSearchCallback callback, String packageName);
  void addUserInteraction(in BranchUniversalObject contentBUO, in String packageName, String userAction, String contentUrl);
  void getTopRecommendedApps(int count, boolean skipSystemApps, String packageaName);
}
