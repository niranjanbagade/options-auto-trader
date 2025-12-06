package com.trading.automated.nb.AutoTrader.services;

import com.trading.automated.nb.AutoTrader.cache.GlobalContextStore;
import com.trading.automated.nb.AutoTrader.services.brokers.ZerodhaTradeService;
import com.zerodhatech.kiteconnect.KiteConnect;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ZerodhaTradingAccountTest {
    @InjectMocks
    ZerodhaTradeService zerodhaTradingAccount;
    @Mock
    KiteConnect kiteConnect;
    @Mock
    GlobalContextStore globalContextStore;
    @BeforeEach
    void setUp() {
//        zerodhaTradingAccount.LOT_SIZE = 75;
    }
}