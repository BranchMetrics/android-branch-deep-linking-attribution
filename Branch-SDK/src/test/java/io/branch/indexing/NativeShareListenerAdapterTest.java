package io.branch.indexing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;

/**
 * EMT-3881: showShareSheet() adapts the two-argument native share callbacks onto the
 * three-argument BranchLinkShareListener that 5.x integrations implement. The channel is only
 * available because SharingBroadcastReceiver reports the chosen app (onChannelSelected) before it
 * reports completion (onLinkShareResponse), so these pin that the adapter carries the value across
 * the two calls rather than dropping it.
 */
public class NativeShareListenerAdapterTest {

    /** Records what the 5.x-style listener actually received. */
    private static class RecordingListener implements Branch.BranchLinkShareListener {
        String sharedLink;
        String channel;
        BranchError error;
        final List<String> channelsSelected = new ArrayList<>();
        int shareResponseCount = 0;

        @Override
        public void onShareLinkDialogLaunched() { }

        @Override
        public void onShareLinkDialogDismissed() { }

        @Override
        public void onLinkShareResponse(String sharedLink, String channel, BranchError error) {
            this.sharedLink = sharedLink;
            this.channel = channel;
            this.error = error;
            shareResponseCount++;
        }

        @Override
        public void onChannelSelected(String channelName) {
            channelsSelected.add(channelName);
        }
    }

    private static final String WHATSAPP =
            "ComponentName{com.whatsapp/com.whatsapp.ContactPicker}";

    @Test
    public void reportsTheChannelSelectedBeforeTheShareResponse() {
        RecordingListener listener = new RecordingListener();
        BranchUniversalObject.NativeShareListenerAdapter adapter =
                new BranchUniversalObject.NativeShareListenerAdapter(listener);

        // The order SharingBroadcastReceiver invokes them in.
        adapter.onChannelSelected(WHATSAPP);
        adapter.onLinkShareResponse("https://bnc.lt/abc", null);

        assertEquals("channel selected before completion must reach onLinkShareResponse",
                WHATSAPP, listener.channel);
        assertEquals("https://bnc.lt/abc", listener.sharedLink);
        assertNull(listener.error);
    }

    @Test
    public void reportsNullChannelWhenNoAppWasEverChosen() {
        RecordingListener listener = new RecordingListener();
        BranchUniversalObject.NativeShareListenerAdapter adapter =
                new BranchUniversalObject.NativeShareListenerAdapter(listener);

        // Link creation failed, so the sheet never opened and onChannelSelected never fired.
        BranchError error = new BranchError("Trouble creating a URL.",
                BranchError.ERR_BRANCH_NO_CONNECTIVITY);
        adapter.onLinkShareResponse(null, error);

        assertNull("no app chosen means no channel to report", listener.channel);
        assertEquals(error, listener.error);
    }

    @Test
    public void stillForwardsOnChannelSelectedToTheCaller() {
        RecordingListener listener = new RecordingListener();
        BranchUniversalObject.NativeShareListenerAdapter adapter =
                new BranchUniversalObject.NativeShareListenerAdapter(listener);

        adapter.onChannelSelected(WHATSAPP);

        assertEquals("capturing the channel must not swallow the callback",
                1, listener.channelsSelected.size());
        assertEquals(WHATSAPP, listener.channelsSelected.get(0));
        assertEquals("onChannelSelected must not fire the completion callback",
                0, listener.shareResponseCount);
    }

    @Test
    public void lastChannelSelectedWins() {
        RecordingListener listener = new RecordingListener();
        BranchUniversalObject.NativeShareListenerAdapter adapter =
                new BranchUniversalObject.NativeShareListenerAdapter(listener);

        adapter.onChannelSelected("ComponentName{com.foo/com.foo.Picker}");
        adapter.onChannelSelected(WHATSAPP);
        adapter.onLinkShareResponse("https://bnc.lt/abc", null);

        assertEquals("the most recent selection is the one that was shared to",
                WHATSAPP, listener.channel);
    }
}
