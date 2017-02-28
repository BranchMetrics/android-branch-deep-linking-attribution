// IBridgeInterface.aidl
package io.branch.search;
import io.branch.indexing.BranchUniversalObject;
import io.branch.search.BranchSearchResult;
import io.branch.search.ContentResult;

interface IBranchSearchCallback {
  void onSearchResult(in int offset, in int limit, in String searchKey, in BranchSearchResult searchResult);
  void onRecommendedAppList(in List<String> packageNames);
  void onRecommendedContent(in List<ContentResult> recommendedContents);
}
