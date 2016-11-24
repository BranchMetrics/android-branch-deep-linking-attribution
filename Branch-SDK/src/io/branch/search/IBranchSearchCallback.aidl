// IBridgeInterface.aidl
package io.branch.search;
import io.branch.indexing.BranchUniversalObject;
import io.branch.search.BranchSearchContent;

interface IBranchSearchCallback {
  void onSearchResult(in int offset, in int limit, in String searchKey, in List<BranchSearchContent> searchResult);
}
