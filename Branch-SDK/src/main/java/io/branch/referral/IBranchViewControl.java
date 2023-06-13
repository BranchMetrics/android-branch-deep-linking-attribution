package io.branch.referral;

/**
 * Interface for defining optional Branch view behaviour for Activities
 */
public interface IBranchViewControl {
    /**
     * Defines if an activity is interested to show Branch views or not.
     * By default activities are considered as Branch view enabled. In case of activities which are not interested to show a Branch view (Splash screen for example)
     * should implement this and return false. The pending Branch view will be shown with the very next Branch view enabled activity
     *
     * @return A {@link Boolean} whose value is true if the activity don't want to show any Branch view.
     */
    boolean skipBranchViewsOnThisActivity();
}
