package fi.hel.verkkokauppa.payment.testing;

import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.common.rest.dto.configuration.MerchantDto;
import fi.hel.verkkokauppa.payment.testing.annotations.RunIfProfile;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunIfProfile(profile = "local")
@RunWith(SpringRunner.class)
@SpringBootTest
public abstract class BaseFunctionalTest {

    @Autowired
    protected CommonServiceConfigurationClient commonServiceConfigurationClient;

    public String getFirstMerchantIdFromNamespace(String namespace) {
        List<MerchantDto> merchants = commonServiceConfigurationClient.getMerchantsForNamespace(namespace);
        if (merchants.size() > 0) {
            return merchants.get(0).getMerchantId();
        }
        return null;
    }
}
