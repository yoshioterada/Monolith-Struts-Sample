package com.skishop.controller;

import com.skishop.config.TestSecurityConfig;
import com.skishop.model.Address;
import com.skishop.model.PointAccount;
import com.skishop.service.AddressService;
import com.skishop.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AccountController.class)
@Import(TestSecurityConfig.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AddressService addressService;

    @MockBean
    private PointService pointService;

    @Test
    @DisplayName("ポイント残高照会画面が表示される")
    void should_displayPointBalance_when_pointsPageAccessed() throws Exception {
        var account = new PointAccount();
        account.setBalance(1000);
        when(pointService.getAccount("user1")).thenReturn(account);

        mockMvc.perform(get("/account/points")
                        .with(SecurityMockMvcRequestPostProcessors.user("user1").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("account/points"))
                .andExpect(model().attributeExists("pointAccount"));
    }

    @Test
    @DisplayName("住所一覧画面が表示される")
    void should_displayAddressList_when_addressesPageAccessed() throws Exception {
        when(addressService.findByUserId("user1")).thenReturn(List.of(new Address()));

        mockMvc.perform(get("/account/addresses")
                        .with(SecurityMockMvcRequestPostProcessors.user("user1").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("account/addresses"))
                .andExpect(model().attributeExists("addresses", "addressRequest"));
    }

    @Test
    @DisplayName("有効な住所を追加するとリダイレクトされる")
    void should_redirectToAddresses_when_validAddressSubmitted() throws Exception {
        when(addressService.findByUserId("user1")).thenReturn(List.of());

        mockMvc.perform(post("/account/addresses")
                        .with(SecurityMockMvcRequestPostProcessors.user("user1").roles("USER"))
                        .param("label", "Home")
                        .param("recipientName", "Test User")
                        .param("postalCode", "100-0001")
                        .param("prefecture", "Tokyo")
                        .param("address1", "Chiyoda-ku 1-1")
                        .param("address2", "")
                        .param("phone", "03-1234-5678")
                        .param("isDefault", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account/addresses"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(addressService).createFromRequest(any(), eq("user1"));
    }

    @Test
    @DisplayName("バリデーションエラー時は住所一覧画面が再表示される")
    void should_returnAddressForm_when_validationErrors() throws Exception {
        when(addressService.findByUserId("user1")).thenReturn(List.of());

        mockMvc.perform(post("/account/addresses")
                        .with(SecurityMockMvcRequestPostProcessors.user("user1").roles("USER"))
                        .param("label", "")
                        .param("recipientName", "")
                        .param("postalCode", "")
                        .param("prefecture", "")
                        .param("address1", "")
                        .param("address2", "")
                        .param("phone", "")
                        .param("isDefault", "false"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/addresses"))
                .andExpect(model().attributeExists("addresses"));
    }

    @Test
    @DisplayName("住所を削除するとリダイレクトされる")
    void should_redirectToAddresses_when_addressDeleted() throws Exception {
        mockMvc.perform(delete("/account/addresses/addr-1")
                        .with(SecurityMockMvcRequestPostProcessors.user("user1").roles("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account/addresses"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(addressService).deleteByIdAndUserId("addr-1", "user1");
    }
}
