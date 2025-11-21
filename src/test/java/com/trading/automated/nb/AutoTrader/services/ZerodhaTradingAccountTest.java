package com.trading.automated.nb.AutoTrader.services;

import com.trading.automated.nb.AutoTrader.cache.GlobalContextStore;
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