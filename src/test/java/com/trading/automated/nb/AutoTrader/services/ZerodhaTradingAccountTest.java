package com.trading.automated.nb.AutoTrader.services;

import com.trading.automated.nb.AutoTrader.cache.GlobalContextStore;
import com.trading.automated.nb.AutoTrader.telegram.TelegramAckService;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.kiteconnect.utils.Constants;
import com.zerodhatech.models.Order;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZerodhaTradingAccountTest {

    @InjectMocks
    ZerodhaTradingAccount zerodhaTradingAccount;

    @Mock
    KiteConnect kiteConnect;
    @Mock
    GlobalContextStore globalContextStore;
    @Mock
    TelegramAckService telegramAckService;

    @BeforeEach
    void setUp() {
//        zerodhaTradingAccount.LOT_SIZE = 75;
        zerodhaTradingAccount.lots = 2;
        zerodhaTradingAccount.isTesting = false;
    }

    @Test
    void testPlaceOrder_TestingMode() throws Exception, KiteException {
        zerodhaTradingAccount.isTesting = true;
        String result = zerodhaTradingAccount.placeOrder("RELIANCE244172600CE", "BUY", "CE", false);
        assertEquals("TEST_ORDER_ID_2727", result);
        verify(telegramAckService).postMessage(contains("Testing mode"));
        verifyNoInteractions(kiteConnect);
        zerodhaTradingAccount.isTesting = false;
    }

    @Test
    void testPlaceOrder_SquareOff_NoPosition() throws Exception, KiteException {
        when(globalContextStore.containsKey(anyString())).thenReturn(false);
        String result = zerodhaTradingAccount.placeOrder("NIFTY2450918000PE", "SELL", "PE", true);
        assertEquals("NO_POSITION_TO_SQUARE_OFF", result);
        verify(telegramAckService).postMessage(contains("No existing position to square off"));
        verifyNoInteractions(kiteConnect);
    }

    @Test
    void testPlaceOrder_SquareOff_WithPosition_Success() throws Exception, KiteException {
        when(globalContextStore.containsKey(anyString())).thenReturn(true);
        Order order = new Order(); order.orderId = "SOFF123";
        when(kiteConnect.placeOrder(any(), eq(Constants.ORDER_TYPE_MARKET))).thenReturn(order);

        String result = zerodhaTradingAccount.placeOrder("BANKNIFTY2451622800CE", "SELL", "CE", true);

        assertEquals("SOFF123", result);
        verify(globalContextStore).removeKey("BANKNIFTY2451622800CE");
        verify(telegramAckService, never()).postMessage(contains("Order placed"));
    }

    @Test
    void testPlaceOrder_SquareOff_WithPosition_KiteException_NoRetry() throws Exception, KiteException {
        when(globalContextStore.containsKey(anyString())).thenReturn(true);
        KiteException ke = new KiteException("insufficient funds");
        when(kiteConnect.placeOrder(any(), eq(Constants.ORDER_TYPE_MARKET))).thenThrow(ke);
        KiteException thrown = assertThrows(KiteException.class, () -> zerodhaTradingAccount.placeOrder("NIFTY2450919000PE", "SELL", "PE", true));
        assertSame(ke, thrown);
    }

    @Test
    void testPlaceOrder_SquareOff_WithPosition_KiteException_RetryThenGiveUp() throws Exception, KiteException {
        when(globalContextStore.containsKey(anyString())).thenReturn(true);
        KiteException ke = new KiteException("Something else");
        when(kiteConnect.placeOrder(any(), eq(Constants.ORDER_TYPE_MARKET))).thenThrow(ke);
        KiteException thrown = assertThrows(KiteException.class, () -> zerodhaTradingAccount.placeOrder("NIFTY2450919100PE", "SELL", "PE", true));
        assertSame(ke, thrown);
    }

    @Test
    void testPlaceOrder_SquareOff_WithPosition_IOException_RetryThenGiveUp() throws Exception, KiteException {
        when(globalContextStore.containsKey(anyString())).thenReturn(true);
        when(kiteConnect.placeOrder(any(), eq(Constants.ORDER_TYPE_MARKET)))
                .thenThrow(new IOException("network error"))
                .thenThrow(new IOException("network error"))
                .thenThrow(new IOException("network error"));
        assertThrows(IOException.class, () ->
            zerodhaTradingAccount.placeOrder("NIFTY2450919200PE", "SELL", "PE", true)
        );
    }

    @Test
    void testPlaceOrder_FreshOrder_Success() throws Exception, KiteException {
        Order order = new Order(); order.orderId = "NEW12345";
        when(kiteConnect.placeOrder(any(), eq(Constants.ORDER_TYPE_MARKET))).thenReturn(order);
        String result = zerodhaTradingAccount.placeOrder("RELIANCE244172900CE", "BUY", "CE", false);
        assertEquals("NEW12345", result);
        verify(globalContextStore).setValue("RELIANCE244172900CE", Constants.ORDER_TYPE_MARKET);
        verify(telegramAckService).postMessage(contains("Order placed"));
    }

    @Test
    void testPlaceOrder_FreshOrder_AMOConversionError() throws Exception, KiteException {
        KiteException amoEx = new KiteException("Your order could not be converted to a After Market Order");
        when(kiteConnect.placeOrder(any(), eq(Constants.ORDER_TYPE_MARKET))).thenThrow(amoEx);
        KiteException thrown = assertThrows(KiteException.class, () ->
            zerodhaTradingAccount.placeOrder("SBIN244172000PE", "BUY", "PE", false)
        );
        assertSame(amoEx, thrown);
    }

    @Test
    void testPlaceOrder_FreshOrder_MarketClosedError() throws Exception, KiteException {
        KiteException mktClosed = new KiteException("Markets are closed right now");
        when(kiteConnect.placeOrder(any(), eq(Constants.ORDER_TYPE_MARKET))).thenThrow(mktClosed);
        KiteException thrown = assertThrows(KiteException.class, () ->
            zerodhaTradingAccount.placeOrder("TCS244173000CE", "BUY", "CE", false)
        );
        assertSame(mktClosed, thrown);
    }

    @Test
    void testPlaceOrder_FreshOrder_InsufficientFunds() throws Exception, KiteException {
        KiteException fundsEx = new KiteException("insufficient funds");
        when(kiteConnect.placeOrder(any(), eq(Constants.ORDER_TYPE_MARKET))).thenThrow(fundsEx);
        KiteException thrown = assertThrows(KiteException.class, () ->
            zerodhaTradingAccount.placeOrder("SBIN244172500PE", "SELL", "PE", false)
        );
        assertSame(fundsEx, thrown);
    }

    @Test
    void testPlaceOrder_FreshOrder_UnknownKiteException_RetryAndSucceed() throws Exception, KiteException {
        // 2 attempts fail, then success
        when(kiteConnect.placeOrder(any(), eq(Constants.ORDER_TYPE_MARKET)))
            .thenThrow(new KiteException("Some issue"))
            .thenThrow(new IOException("io1"))
            .then((inv) -> {
                Order o = new Order(); o.orderId = "SUCCEED_AFTER_RETRY";
                return o;
            });
        String res = zerodhaTradingAccount.placeOrder("TCS245002900CE", "BUY", "CE", false);
        assertEquals("SUCCEED_AFTER_RETRY", res);
    }

    @Test
    void testPlaceOrder_ParamMapping() throws Exception, KiteException {
        ArgumentCaptor<com.zerodhatech.models.OrderParams> captor =
                ArgumentCaptor.forClass(com.zerodhatech.models.OrderParams.class);

        Order order = new Order(); order.orderId = "AAA";
        when(kiteConnect.placeOrder(captor.capture(), eq(Constants.ORDER_TYPE_MARKET))).thenReturn(order);

        zerodhaTradingAccount.placeOrder("ITC2450029400PE", "SELL", "PE", false);

        com.zerodhatech.models.OrderParams params = captor.getValue();
        assertEquals(Constants.EXCHANGE_NFO, params.exchange);
        assertEquals("ITC2450029400PE", params.tradingsymbol);
        assertEquals(Constants.TRANSACTION_TYPE_SELL, params.transactionType);
        assertEquals(2 * 75, params.quantity);
        assertEquals(Constants.ORDER_TYPE_MARKET, params.orderType);
        assertEquals(Constants.PRODUCT_NRML, params.product);

        verify(globalContextStore).setValue("ITC2450029400PE", Constants.ORDER_TYPE_MARKET);
        verify(telegramAckService).postMessage(contains("Order placed"));
    }
}