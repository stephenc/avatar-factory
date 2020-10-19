package io.github.stephenc.avatar.factory;

import com.github.javafaker.Faker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvatarBuilderTest {
    @Test
    void given__faker_generated_names__when__used_as_seed__then__template_groups_mostly_equally_likely() {
        // given
        Faker faker = Faker.instance();

        // when
        int count = 0;
        for (int attempt = 0; attempt < 10_000; attempt++) {
            AvatarBuilder builder = new AvatarBuilder(faker.name().name());
            if (builder.getHead() == AvatarBuilder.Head.FEMALE) {
                count++;
            }
        }
        int deviation = count - 5000;

        // then

        // When N > 20 a binomial distribution is reasonably approximated by normal distribution with
        // mean NP and variance NP(1-P).
        // In our case we hope P = 0.5 and N is 10,000
        // i.e. mean = 5000, variance = 2500, std deviation = 50
        // A one sided cumulative normal distribution has 3.075 std deviation at 0.9990
        // We expect 1 in 2000 times the following assertion will fail in the absence of bias
        assertTrue(Math.abs(deviation) < 150, "Expected to fail 1 in 2000 runs for unbiased group selection");

        // If there was 5% bias, i.e. P = 0.55 or symmetrically P = 0.45
        // mean = 5500, variance = 2475, std deviation ~ 50
        assertFalse(Math.abs(count - 5500) < 150);
        assertFalse(Math.abs(count - 4500) < 150);

        // NOTE this test does not have the power to detect a 2% bias
        // If there was 2% bias, i.e. P = 0.52 or symmetrically P = 0.48
        // mean = 5200, variance = 2496, std deviation ~ 50
        // there is a 30% chance of a normally distributed unbiased generator being more than 1 standard deviation
        // from its mean, which would bring it into the range that is 3 standard deviations from a 2% biased generator
    }

}
