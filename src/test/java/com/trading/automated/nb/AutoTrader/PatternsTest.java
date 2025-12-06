package com.trading.automated.nb.AutoTrader;

import com.trading.automated.nb.AutoTrader.enums.Patterns;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class PatternsTest {
//
//    @Test
//    public void testEntryPattern() {
//        String msg = "\"BUY\" 21 OCT \"NIFTY 25900 CE\" between 145-135";
//        assertTrue(Patterns.ENTRY_PATTERN.matcher(msg).find(), "ENTRY pattern failed");
//    }
//
//    @Test
//    public void testSquareOffPatterns() {
//        List<String> cases = List.of(
//            // 1–5 directional trade closures (dual leg)
//            "SQUARE OFF\nModify stop loss and book 100% profit. Sell 25500 CE @ 66 and Buy 25500 PE @ 77",
//            "SQUARE OFF\nModify stop loss and book 50% profit and now keep trailing stop loss at cost for remaining 50% qty. Sell 25500 CE @ 66 and Buy 25500 PE @ 77",
//            "SQUARE OFF\nTrailing stop loss triggered. Modify stop loss and book profit for remaining 50% quantity. Sell 25500 CE @ 66 and Buy 25500 PE @ 77",
//            "SQUARE OFF\nTrailing stop loss triggered. Square off position. Sell 25500 CE @ 66 and Buy 25500 PE @ 77",
//            "SQUARE OFF\nStop loss triggered. Modify your stop loss and square off position. Sell 25500 CE @ 66 and Buy 25500 PE @ 77",
//
//            // 6–15 exit bought positions (single leg Sell)
//            "SQUARE OFF\nModify stop loss and book 100% profit. Sell 25500 CE @ 45",
//            "SQUARE OFF\nModify stop loss and book 50% profit and now keep trailing stop loss at cost for remaining 50% qty. Sell 25500 CE @ 45",
//            "SQUARE OFF\nTrailing stop loss triggered. Modify stop loss and book profit for remaining 50% quantity. Sell 25500 CE @ 45",
//            "SQUARE OFF\nTrailing stop loss triggered. Square off position. Sell 25500 CE @ 45",
//            "SQUARE OFF\nStop loss triggered. Modify your stop loss and square off position. Sell 25500 CE @ 45",
//
//            // 11–15 exit bought positions (single leg Sell PE)
//            "SQUARE OFF\nModify stop loss and book 100% profit. Sell 25500 PE @ 45",
//            "SQUARE OFF\nModify stop loss and book 50% profit and now keep trailing stop loss at cost for remaining 50% qty. Sell 25500 PE @ 45",
//            "SQUARE OFF\nTrailing stop loss triggered. Modify stop loss and book profit for remaining 50% quantity. Sell 25500 PE @ 45",
//            "SQUARE OFF\nTrailing stop loss triggered. Square off position. Sell 25500 PE @ 45",
//            "SQUARE OFF\nStop loss triggered. Modify your stop loss and square off position. Sell 25500 PE @ 45",
//
//            // 16–25 exit sold positions (dual Buy or single Buy)
//            "SQUARE OFF\nModify stop loss and book 100% profit. Buy 25500 CE @ 45",
//            "SQUARE OFF\nModify stop loss and book 50% profit and now keep trailing stop loss at cost for remaining 50% qty. Buy 25500 CE @ 45",
//            "SQUARE OFF\nTrailing stop loss triggered. Modify stop loss and book profit for remaining 50% quantity. Buy 25500 CE @ 45",
//            "SQUARE OFF\nTrailing stop loss triggered. Square off position. Buy 25500 CE @ 45",
//            "SQUARE OFF\nStop loss triggered. Modify your stop loss and square off position. Buy 25500 CE @ 45",
//
//            "SQUARE OFF\nModify stop loss and book 100% profit. Buy 25500 PE @ 45",
//            "SQUARE OFF\nModify stop loss and book 50% profit and now keep trailing stop loss at cost for remaining 50% qty. Buy 25500 PE @ 45",
//            "SQUARE OFF\nTrailing stop loss triggered. Modify stop loss and book profit for remaining 50% quantity. Buy 25500 PE @ 45",
//            "SQUARE OFF\nTrailing stop loss triggered. Square off position. Buy 25500 PE @ 45",
//            "SQUARE OFF\nStop loss triggered. Modify your stop loss and square off position. Buy 25500 PE @ 45"
//        );
//
//        for (int i = 0; i < cases.size(); i++) {
//            String msg = cases.get(i);
//            boolean found = Patterns.SQUARE_OFF_PATTERN.matcher(msg).find();
//            assertTrue(found, "Failed case #" + (i + 1) + ":\n" + msg);
//        }
//    }
}
