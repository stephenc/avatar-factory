package io.github.stephenc.avatar.factory;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Renders an SVG avatar.
 */
public class AvatarBuilder {
    private static final String NOSE = load("common/Nose");
    private static final String EYES = load("common/Eyes");
    private static final String BACKGROUND = load("common/Background");
    private static final String AVATAR = load("Avatar");
    @NonNull
    private final String name;
    @CheckForNull
    private Accessory accessory;
    @NonNull
    private Color accessoryColor;
    @CheckForNull
    private Color backgroundColor;
    @NonNull
    private Color backgroundSecondaryColor;
    @CheckForNull
    private Clothes clothes;
    @NonNull
    private Color clothesColor;
    @NonNull
    private Color clothesSecondaryColor;
    @NonNull
    private Eyes eyes;
    @NonNull
    private EyesColor eyesColor;
    @CheckForNull
    private Glasses glasses;
    @NonNull
    private Color glassesColor;
    @CheckForNull
    private FacialHair facialHair;
    @NonNull
    private HairColor facialHairColor;
    @NonNull
    private Head head;
    @CheckForNull
    private Hair hair;
    @NonNull
    private HairColor hairColor;
    @NonNull
    private Mouth mouth;
    @NonNull
    private LipColor mouthColor;
    @NonNull
    private SkinColor noseColor;
    @NonNull
    private SkinColor skinColor;

    /**
     * Creates an empty template avatar.
     *
     * @param name the name of the avatar.
     * @param head the head template
     * @param skinColor the skin color
     * @param noseColor the nose color
     */
    public AvatarBuilder(@NonNull String name,
                         @NonNull Head head,
                         @NonNull SkinColor skinColor,
                         @NonNull SkinColor noseColor) {
        this.name = name;
        this.head = head;
        this.noseColor = noseColor;
        this.skinColor = skinColor;
        this.accessory = null;
        this.accessoryColor = Color.BLACK;
        this.backgroundColor = null;
        this.backgroundSecondaryColor = Color.WHITE;
        this.clothes = null;
        this.clothesColor = Color.CONCRETE;
        this.clothesSecondaryColor = Color.GREY;
        this.eyes = Eyes.OPEN;
        this.eyesColor = EyesColor.GREEN_GREY;
        this.glasses = null;
        this.glassesColor = Color.BLACK;
        this.hair = null;
        this.hairColor = HairColor.GREY;
        this.facialHair = null;
        this.facialHairColor = HairColor.GREY;
        this.mouth = Mouth.NORMAL;
        this.mouthColor = LipColor.NO_LIPSTICK;
    }

    /**
     * Creates a default avatar for the given input name, the avatar will have attributes deterministically derived from
     * the name, so the same name will always produce the same avatar, but if you pick random names then every avatar
     * will be different.
     *
     * @param name the name of the avatar.
     */
    public AvatarBuilder(@NonNull String name) {
        this.name = name;
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JLS mandates support for SHA-256, this is not a valid JVM", e);
        }
        md.update(new byte[] {3, 78, -123, -99});
        byte[] seed = md.digest(name.getBytes(StandardCharsets.UTF_8));
        head = pick(seed[0], Arrays.asList(Head.values()));
        accessory = pick(seed[1], matchingAccessories());
        List<Color> colors = new ArrayList<>(Arrays.asList(Color.values()));
        accessoryColor = pick(seed[2], colors);
        colors.remove(accessoryColor);
        clothes = pick(seed[3], matchingClothes());
        clothesColor = pick(seed[4], colors);
        colors.remove(clothesColor);
        clothesSecondaryColor = pick(seed[5], colors);
        colors.remove(clothesSecondaryColor);
        backgroundColor = pick(seed[6], colors);
        colors.remove(backgroundColor);
        backgroundSecondaryColor = pick(seed[7], colors);
        eyes = pick(seed[8], Arrays.asList(Eyes.values()));
        eyesColor = pick(seed[9], Arrays.asList(EyesColor.values()));
        glasses = pick(seed[10], matchingGlasses());
        glassesColor = pick(seed[11], Arrays.asList(Color.values()));
        facialHair = pick(seed[12], matchingFacialHair());
        facialHairColor = pick(seed[13], Arrays.asList(HairColor.values()));
        hair = pick(seed[14], matchingHair());
        hairColor = pick(seed[15], Arrays.asList(HairColor.values()));
        mouth = pick(seed[16], Arrays.asList(Mouth.values()));
        mouthColor = pick(seed[17], matchingLipColor());
        List<SkinColor> skinColors = new ArrayList<>(Arrays.asList(SkinColor.values()));
        skinColor = pick(seed[18], skinColors);
        skinColors.remove(skinColor);
        noseColor = pick(seed[19], skinColors);
    }

    private static <T> T pick(byte random, List<T> options) {
        int n = random ^ (7 * random) ^ (random >> 4) ^ (31 * random >> 2);
        return options.get(Math.abs(n) % options.size());
    }

    private static String load(String template) {
        byte[] buf = new byte[1024];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (InputStream is = AvatarBuilder.class.getResourceAsStream(template + ".svg.hbs")) {
            int len;
            while (-1 != (len = is.read(buf))) {
                bos.write(buf, 0, len);
            }
            return bos.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String darkenColor(String color, float ratio) {
        int r = Math.round(Integer.parseInt(color.substring(1, 3), 16) * (1 - ratio));
        int g = Math.round(Integer.parseInt(color.substring(3, 5), 16) * (1 - ratio));
        int b = Math.round(Integer.parseInt(color.substring(5, 7), 16) * (1 - ratio));
        return String.format("#%02x%02x%02x", Math.max(0, r), Math.max(0, g), Math.max(0, b));
    }

    private static String lightenColor(String color, float ratio) {
        int r = Math.round(Integer.parseInt(color.substring(1, 3), 16) * (1 + ratio));
        int g = Math.round(Integer.parseInt(color.substring(3, 5), 16) * (1 + ratio));
        int b = Math.round(Integer.parseInt(color.substring(5, 7), 16) * (1 + ratio));
        return String.format("#%02x%02x%02x", Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }

    @CheckForNull
    public Accessory getAccessory() {
        return accessory;
    }

    @NonNull
    public AvatarBuilder accessory(@CheckForNull Accessory accessory, @CheckForNull Color color) {
        this.accessory = accessory;
        this.accessoryColor = color == null ? Color.BLACK : color;
        return this;
    }

    @NonNull
    public List<Accessory> matchingAccessories() {
        return Stream.of(Accessory.values())
                .filter(x -> x.templateGroup() == TemplateGroup.COMMON || x.templateGroup() == head.templateGroup())
                .collect(Collectors.toList());
    }

    @NonNull
    public Color getAccessoryColor() {
        return accessoryColor;
    }

    @CheckForNull
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    @NonNull
    public AvatarBuilder background(@CheckForNull Color color, @CheckForNull Color secondaryColor) {
        this.backgroundColor = color;
        this.backgroundSecondaryColor = secondaryColor == null ? Color.WHITE : secondaryColor;
        return this;
    }

    @NonNull
    public Color getBackgroundSecondaryColor() {
        return backgroundSecondaryColor;
    }

    @CheckForNull
    public Clothes getClothes() {
        return clothes;
    }

    @NonNull
    public AvatarBuilder clothes(@CheckForNull Clothes clothes, @CheckForNull Color color,
                                 @CheckForNull Color secondaryColor) {
        this.clothes = clothes;
        this.clothesColor = color == null ? Color.CONCRETE : color;
        this.clothesSecondaryColor = secondaryColor == null ? Color.PETER_RIVER : secondaryColor;
        return this;
    }

    @NonNull
    public List<Clothes> matchingClothes() {
        return Stream.of(Clothes.values())
                .filter(x -> x.templateGroup() == TemplateGroup.COMMON || x.templateGroup() == head.templateGroup())
                .collect(Collectors.toList());
    }

    @CheckForNull
    public Color getClothesColor() {
        return clothesColor;
    }

    @CheckForNull
    public Color getClothesSecondaryColor() {
        return clothesSecondaryColor;
    }

    @NonNull
    public Eyes getEyes() {
        return eyes;
    }

    @NonNull
    public AvatarBuilder eyes(@NonNull Eyes eyes, @NonNull EyesColor eyesColor) {
        this.eyes = eyes;
        this.eyesColor = eyesColor;
        return this;
    }

    @NonNull
    public EyesColor getEyesColor() {
        return eyesColor;
    }

    @CheckForNull
    public Glasses getGlasses() {
        return glasses;
    }

    @NonNull
    public AvatarBuilder glasses(@CheckForNull Glasses glasses, Color color) {
        this.glasses = glasses;
        this.glassesColor = color == null ? Color.BLACK : color;
        return this;
    }

    @NonNull
    public List<Glasses> matchingGlasses() {
        return Stream.of(Glasses.values())
                .filter(x -> x.templateGroup() == TemplateGroup.COMMON || x.templateGroup() == head.templateGroup())
                .collect(Collectors.toList());
    }

    @NonNull
    public Color getGlassesColor() {
        return glassesColor;
    }

    @CheckForNull
    public FacialHair getFacialHair() {
        return facialHair;
    }

    @NonNull
    public AvatarBuilder facialHair(@CheckForNull FacialHair facialHair, @CheckForNull HairColor facialHairColor) {
        this.facialHair = facialHair;
        this.facialHairColor = facialHairColor == null ? HairColor.GREY : facialHairColor;
        return this;
    }

    @NonNull
    public HairColor getFacialHairColor() {
        return facialHairColor;
    }

    @NonNull
    public List<FacialHair> matchingFacialHair() {
        return Stream.of(FacialHair.values())
                .filter(x -> x.templateGroup() == TemplateGroup.COMMON || x.templateGroup() == head.templateGroup())
                .collect(Collectors.toList());
    }

    @NonNull
    public Head getHead() {
        return head;
    }

    @NonNull
    public AvatarBuilder head(@NonNull Head head, @NonNull SkinColor skinColor, @NonNull SkinColor noseColor) {
        this.head = head;
        this.skinColor = skinColor;
        this.noseColor = noseColor;
        return this;
    }

    @CheckForNull
    public Hair getHair() {
        return hair;
    }

    @NonNull
    public AvatarBuilder hair(@CheckForNull Hair hair, @CheckForNull HairColor hairColor) {
        this.hair = hair;
        this.hairColor = hairColor == null ? HairColor.GREY : hairColor;
        return this;
    }

    @NonNull
    public List<Hair> matchingHair() {
        return Stream.of(Hair.values())
                .filter(x -> x.templateGroup() == TemplateGroup.COMMON || x.templateGroup() == head.templateGroup())
                .collect(Collectors.toList());
    }

    @NonNull
    public HairColor getHairColor() {
        return hairColor;
    }

    @NonNull
    public Mouth getMouth() {
        return mouth;
    }

    @NonNull
    public AvatarBuilder mouth(@NonNull Mouth mouth, @NonNull LipColor mouthColor) {
        this.mouth = mouth;
        this.mouthColor = mouthColor;
        return this;
    }

    @NonNull
    public List<LipColor> matchingLipColor() {
        return Stream.of(LipColor.values())
                .filter(x -> x.templateGroup() == TemplateGroup.COMMON || x.templateGroup() == head.templateGroup())
                .collect(Collectors.toList());
    }

    @NonNull
    public LipColor getMouthColor() {
        return mouthColor;
    }

    @NonNull
    public SkinColor getNoseColor() {
        return noseColor;
    }

    @NonNull
    public SkinColor getSkinColor() {
        return skinColor;
    }

    @NonNull
    public String build() throws IOException {
        StringBuilder components = new StringBuilder();
        if (backgroundColor != null) {
            components.append(render(
                    BACKGROUND,
                    new Binding("color", backgroundColor.color),
                    new Binding("lighten", ratio -> lightenColor(backgroundColor.color, Float.parseFloat(ratio))),
                    new Binding("darken", ratio -> darkenColor(backgroundColor.color, Float.parseFloat(ratio))),
                    new Binding("secondaryColor", backgroundSecondaryColor.color)
            ));
        }
        components.append(render(
                head.template,
                new Binding("color", skinColor.color),
                new Binding("lighten", ratio -> lightenColor(skinColor.color, Float.parseFloat(ratio))),
                new Binding("darken", ratio -> darkenColor(skinColor.color, Float.parseFloat(ratio)))
        ));
        components.append(render(
                mouth.template,
                new Binding("color", mouthColor.color),
                new Binding("lighten", ratio -> lightenColor(mouthColor.color, Float.parseFloat(ratio))),
                new Binding("darken", ratio -> darkenColor(mouthColor.color, Float.parseFloat(ratio)))));
        components.append(render(
                NOSE,
                new Binding("color", noseColor.color),
                new Binding("lighten", ratio -> lightenColor(noseColor.color, Float.parseFloat(ratio))),
                new Binding("darken", ratio -> darkenColor(noseColor.color, Float.parseFloat(ratio)))));
        components.append(render(
                EYES,
                new Binding("color", eyesColor.color),
                new Binding("lighten", ratio -> lightenColor(eyesColor.color, Float.parseFloat(ratio))),
                new Binding("darken", ratio -> darkenColor(eyesColor.color, Float.parseFloat(ratio))),
                new Binding("secondaryColor", darkenColor(eyesColor.color, 0.15f)),
                new Binding("gradientId", eyesColor.color.replace('#', '_')),
                new Binding("component", x -> render(eyes.template, new Binding("color", eyesColor.color),
                        new Binding("lighten", ratio -> lightenColor(eyesColor.color, Float.parseFloat(ratio))),
                        new Binding("darken", ratio -> darkenColor(eyesColor.color, Float.parseFloat(ratio))),
                        new Binding("secondaryColor", darkenColor(eyesColor.color, 0.15f)),
                        new Binding("gradientUrl", "url(#" + eyesColor.color.replace('#', '_') + ")")
                ))));
        if (hair != null) {
            components.append(render(
                    hair.outerTemplate,
                    new Binding("color", hairColor.color),
                    new Binding("lighten", ratio -> lightenColor(hairColor.color, Float.parseFloat(ratio))),
                    new Binding("darken", ratio -> darkenColor(hairColor.color, Float.parseFloat(ratio))),
                    new Binding("component", x -> render(hair.template,
                            new Binding("color", hairColor.color),
                            new Binding("lighten", ratio -> lightenColor(hairColor.color, Float.parseFloat(ratio))),
                            new Binding("darken", ratio -> darkenColor(hairColor.color, Float.parseFloat(ratio)))))));
        }
        if (glasses != null) {
            components.append(render(glasses.template,
                    new Binding("color", glassesColor.color),
                    new Binding("lighten", ratio -> lightenColor(glassesColor.color, Float.parseFloat(ratio))),
                    new Binding("darken", ratio -> darkenColor(glassesColor.color, Float.parseFloat(ratio)))));
        }
        if (clothes != null) {
            components.append(render(clothes.template,
                    new Binding("color", clothesColor.color),
                    new Binding("lighten", ratio -> lightenColor(clothesColor.color, Float.parseFloat(ratio))),
                    new Binding("darken", ratio -> darkenColor(clothesColor.color, Float.parseFloat(ratio))),
                    new Binding("secondaryColor", clothesSecondaryColor.color)));
        }
        if (accessory != null) {
            components.append(render(accessory.template,
                    new Binding("color", accessoryColor.color),
                    new Binding("lighten", ratio -> lightenColor(accessoryColor.color, Float.parseFloat(ratio))),
                    new Binding("darken", ratio -> darkenColor(accessoryColor.color, Float.parseFloat(ratio)))));
        }
        if (facialHair != null) {
            components.append(render(facialHair.template,
                    new Binding("color", facialHairColor.color),
                    new Binding("lighten", ratio -> lightenColor(facialHairColor.color, Float.parseFloat(ratio))),
                    new Binding("darken", ratio -> darkenColor(facialHairColor.color, Float.parseFloat(ratio)))));
        }
        return render(AVATAR,
                new Binding("name", name),
                new Binding("components", components.toString())
        );
    }

    private static String render(String template, Binding... bindings) {
        StringBuilder result = new StringBuilder(template.length() + 1024);
        int current = 0;
        int bindingStart;
        while (-1 != (bindingStart = template.indexOf("{{", current))) {
            int bindingEnd = template.indexOf("}}", bindingStart);
            if (bindingEnd == -1) {
                break;
            }
            result.append(template, current, bindingStart);
            int argStart = template.indexOf('(', bindingStart);
            String name;
            String arg;
            if (argStart != -1 && argStart < bindingEnd && template.charAt(bindingEnd - 1) == ')') {
                // we have an argument
                name = template.substring(bindingStart + 2, argStart);
                arg = template.substring(argStart + 1, bindingEnd - 1);
            } else {
                name = template.substring(bindingStart + 2, bindingEnd);
                arg = null;
            }
            result.append(Stream.of(bindings)
                    .filter(b -> name.equals(b.name))
                    .findFirst()
                    .map(b -> b.source.apply(arg))
                    .orElse(template.substring(bindingStart, bindingEnd + 2)));
            current = bindingEnd + 2;
        }
        if (current < template.length()) {
            result.append(template.substring(current));
        }
        return result.toString();
    }

    public enum TemplateGroup {
        COMMON,
        MALE,
        FEMALE
    }

    public enum Head implements Named, TemplateAligned {
        MALE("Male", "male/HeadShape") {
            public TemplateGroup templateGroup() {
                return TemplateGroup.MALE;
            }
        },
        FEMALE("Female", "female/HeadShape") {
            public TemplateGroup templateGroup() {
                return TemplateGroup.FEMALE;
            }
        };

        private final String name;
        private final String template;

        Head(String name, String path) {
            this.name = name;
            this.template = load(path);
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum Color implements Named {
        TURQUOISE("Turquoise", "#1abc9c"),
        EMERLAND("Emerland", "#2ecc71"),
        PETER_RIVER("Peter river", "#3498db"),
        AMETHYST("Amethyst", "#9b59b6"),
        WET_ASPHALT("Wet asphalt", "#34495e"),
        GREEN_SEA("Green sea", "#16a085"),
        NEPHRITIS("Nephritis", "#27ae60"),
        BELIZE_HOLE("Belize hole", "#2980b9"),
        WISTERIA("Wisteria", "#8e44ad"),
        MIDNIGHT_BLUE("Midnight blue", "#2c3e50"),
        SUN_FLOWER("Sun flower", "#f1c40f"),
        CARROT("Carrot", "#e67e22"),
        ALIZARIN("Alizarin", "#e74c3c"),
        CLOUDS("Clouds", "#ecf0f1"),
        CONCRETE("Concrete", "#95a5a6"),
        ORANGE("Orange", "#f39c12"),
        PUMKIN("Pumkin", "#d35400"),
        POMEGRATE("Pomegrate", "#c0392b"),
        SILVER("Silver", "#bdc3c7"),
        SABESTOS("Sabestos", "#7f8c8d"),
        BLACK("Black", "#000000"),
        GREY("Grey", "#333333"),
        WHITE("White", "#FFFFFF");

        private final String name;

        private final String color;

        Color(String name, String color) {
            this.name = name;
            this.color = color;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum LipColor implements Named, TemplateAligned {
        NO_LIPSTICK("No lipstick", "#ef843b"),
        LADY_DANGER("Lady danger", "#DD2F28"),
        RUSSIAN_RED("Russian red", "#63171D"),
        RED("Red", "#7F0E0F"),
        SELLECTSAINT("Sellectsaint", "#AD3468"),
        WHAM("Wham", "#C85F41"),
        GLARINGLYHIP("Glaringlyhip", "#972E1A"),
        PINK_NOUVEAU("Pink nouveau", "#B03761"),
        VIVA_NICKY("Viva Nicky", "#C13539"),
        POWERFUL("Powerful", "#B84F40"),
        SHOW_ORCHID("Show orchid", "#CD6871"),
        FUSION_PINK("Fusion pink", "#DA6798"),
        RUBY_WOO("Ruby woo", "#C71418"),
        CANDY_YUM_YUM("Candy yum yum", "#FD3686"),
        MORANGE("Morange", "#B6301E"),
        GERMAIN("Germain", "#AE0923"),
        MELTDOWN("Meltdown", "#BD503C"),
        CYBER("Cyber", "#22171A"),
        BRIGHT_PINK("Bright pink", "#F659A2"),
        BUBBLEGUM("Bubblegum", "#B26991"),
        LADYBUG("Ladybug", "#C73024");

        private final String name;

        private final String color;

        LipColor(String name, String color) {
            this.name = name;
            this.color = color;
        }


        @Override
        public TemplateGroup templateGroup() {
            return this == NO_LIPSTICK ? TemplateGroup.COMMON : TemplateGroup.FEMALE;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum SkinColor implements Named {
        VERY_PALE("Very pale", "#FDDAC5"),
        PALE("Pale", "#FBD2B4"),
        LIGHT_TAN("Light tan", "#FCCC95"),
        TAN("Tan", "#E2B182"),
        BROWN("Brown", "#DBA582"),
        DARK_BROWN("Dark brown", "#A96C4F");

        private final String name;

        private final String color;

        SkinColor(String name, String color) {
            this.name = name;
            this.color = color;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum Accessory implements Named, TemplateAligned {
        COMMON_A("No accessory", "common/accessory/TypeA", TemplateGroup.COMMON),
        COMMON_B("Headphones", "common/accessory/TypeB", TemplateGroup.COMMON),
        FEMALE_A("Necklace", "female/accessory/TypeA", TemplateGroup.FEMALE),
        FEMALE_B("Headdress", "female/accessory/TypeB", TemplateGroup.FEMALE);

        private final String name;
        private final String template;
        private final TemplateGroup templateGroup;

        Accessory(String name, String path, TemplateGroup templateGroup) {
            this.name = name;
            this.template = load(path);
            this.templateGroup = templateGroup;
        }

        @Override
        public TemplateGroup templateGroup() {
            return templateGroup;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum Clothes implements Named, TemplateAligned {
        MALE_A("Hoodie", "male/clothes/TypeA", TemplateGroup.MALE),
        MALE_B("T-shirt", "male/clothes/TypeB", TemplateGroup.MALE),
        MALE_C("V-shirt", "male/clothes/TypeC", TemplateGroup.MALE),
        MALE_D("Formal", "male/clothes/TypeD", TemplateGroup.MALE),
        MALE_E("Pullover", "male/clothes/TypeE", TemplateGroup.MALE),
        MALE_F("To neck", "male/clothes/TypeF", TemplateGroup.MALE),
        FEMALE_A("Formal", "female/clothes/TypeA", TemplateGroup.FEMALE),
        FEMALE_B("Top", "female/clothes/TypeB", TemplateGroup.FEMALE),
        FEMALE_C("Elegant", "female/clothes/TypeC", TemplateGroup.FEMALE),
        FEMALE_D("T-shirt", "female/clothes/TypeD", TemplateGroup.FEMALE),
        FEMALE_E("Dress", "female/clothes/TypeE", TemplateGroup.FEMALE),
        FEMALE_F("V-shirt", "female/clothes/TypeF", TemplateGroup.FEMALE);

        private final String name;
        private final String template;
        private final TemplateGroup templateGroup;

        Clothes(String name, String path, TemplateGroup templateGroup) {
            this.name = name;
            this.template = load(path);
            this.templateGroup = templateGroup;
        }

        @Override
        public TemplateGroup templateGroup() {
            return templateGroup;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum Eyes implements Named {
        OPEN("Open", "common/eyes/TypeA"),
        WINK_LEFT("Wink left", "common/eyes/TypeB"),
        WINK_RIGHT("Wink right", "common/eyes/TypeC"),
        HAPPY("Happy", "common/eyes/TypeD");
        private final String name;
        private final String template;

        Eyes(String name, String path) {
            this.name = name;
            this.template = load(path);
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum EyesColor implements Named {
        BROWN("Brown", "#634e34"),
        BLUE("Blue", "#2e536f"),
        LIGHT_BLUE("Light blue", "#A6B6C2"),
        GREEN("Green", "#3d671d"),
        LIGHT_GREEN("Light green", "#1c7847"),
        GREEN_GREY("Green-grey", "#497665");

        private final String name;

        private final String color;

        EyesColor(String name, String color) {
            this.name = name;
            this.color = color;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum Glasses implements Named, TemplateAligned {
        COMMON_A("No glasses", "common/glasses/TypeA", TemplateGroup.COMMON),
        COMMON_B("Big", "common/glasses/TypeB", TemplateGroup.COMMON),
        COMMON_C("Sunglasses", "common/glasses/TypeC", TemplateGroup.COMMON),
        COMMON_D("Oldschool", "common/glasses/TypeD", TemplateGroup.COMMON),
        MALE_A("Elegant", "male/glasses/TypeA", TemplateGroup.MALE),
        FEMALE_A("Elegant", "female/glasses/TypeA", TemplateGroup.FEMALE);
        private final String name;
        private final String template;
        private final TemplateGroup templateGroup;

        Glasses(String name, String path, TemplateGroup templateGroup) {
            this.name = name;
            this.template = load(path);
            this.templateGroup = templateGroup;
        }

        @Override
        public TemplateGroup templateGroup() {
            return templateGroup;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum Hair implements Named, TemplateAligned {
        MALE_A("To side", "male/hair/TypeA", TemplateGroup.MALE),
        MALE_B("Decent", "male/hair/TypeB", TemplateGroup.MALE),
        MALE_C("Formal", "male/hair/TypeC", TemplateGroup.MALE),
        MALE_D("Disheveled", "male/hair/TypeD", TemplateGroup.MALE),
        MALE_E("Longer", "male/hair/TypeE", TemplateGroup.MALE),
        MALE_F("Hipster", "male/hair/TypeF", TemplateGroup.MALE),
        MALE_G("Ups", "male/hair/TypeG", TemplateGroup.MALE),
        FEMALE_A("Wavy", "female/hair/TypeA", TemplateGroup.FEMALE),
        FEMALE_B("Bun", "female/hair/TypeB", TemplateGroup.FEMALE),
        FEMALE_C("Short", "female/hair/TypeC", TemplateGroup.FEMALE),
        FEMALE_D("Curly", "female/hair/TypeD", TemplateGroup.FEMALE),
        FEMALE_E("Elegant", "female/hair/TypeE", TemplateGroup.FEMALE),
        FEMALE_F("Mikado", "female/hair/TypeF", TemplateGroup.FEMALE),
        FEMALE_G("Straight long", "female/hair/TypeG", TemplateGroup.FEMALE);
        private final String name;
        private final String template;
        private final String outerTemplate;
        private final TemplateGroup templateGroup;

        Hair(String name, String path, TemplateGroup templateGroup) {
            this.name = name;
            this.template = load(path);
            this.outerTemplate = load(templateGroup == TemplateGroup.MALE ? "male/Hair" : "female/Hair");
            this.templateGroup = templateGroup;
        }

        @Override
        public TemplateGroup templateGroup() {
            return templateGroup;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum HairColor implements Named {
        BLACK("Black", "#090806"),
        OFF_BLACK("Off-black", "#2C222B"),
        DARK_GREY("Dark grey", "#71635A"),
        GREY("Grey", "#B7A69E"),
        LIGHT_GREY("Light grey", "#D6C4C2"),
        PLATINUM_BLONDE("Platinum Blonde", "#CABFB1"),
        WHITE_BLONDE("White Blonde", "#FFF5E1"),
        LIGHT_BLONDE("Light Blonde", "#E6CEA8"),
        GOLDEN_BLONDE("Golden Blonde", "#E5C8A8"),
        ASH_BLONDE("Ash Blonde", "#DEBC99"),
        HONEY_BLONDE("Honey Blonde", "#B89778"),
        STRAWBERRY_BLONDE("Strawberry Blonde", "#A56B46"),
        LIGHT_RED("Light Red", "#B55239"),
        DARK_RED("Dark Red", "#8D4A43"),
        LIGHT_AUBURN("Light Auburn", "#91553D"),
        DARK_AUBURN("Dark Auburn", "#533D32"),
        DARK_BROWN("Dark Brown", "#3B3024"),
        GOLDEN_BROWN("Golden Brown", "#554838"),
        MEDIUM_BROWN("Medium Brown", "#4E433F"),
        BROWN("Brown", "#6A4E42"),
        LIGHT_BROWN("Light Brown", "#A7856A"),
        ASH_BROWN("Ash Brown", "#977961");

        private final String name;

        private final String color;

        HairColor(String name, String color) {
            this.name = name;
            this.color = color;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum FacialHair implements Named, TemplateAligned {
        NONE("No facial hair", "male/facial-hair/TypeA", TemplateGroup.COMMON),
        BEARD("Beard", "male/facial-hair/TypeB", TemplateGroup.MALE),
        HIPSTER("Hipster", "male/facial-hair/TypeC", TemplateGroup.MALE),
        STUBBLE("Stubble", "male/facial-hair/TypeD", TemplateGroup.MALE),
        MOUSTASHE("Moustashe", "male/facial-hair/TypeE", TemplateGroup.MALE);

        private final String name;
        private final String template;
        private final TemplateGroup templateGroup;

        FacialHair(String name, String path, TemplateGroup templateGroup) {
            this.name = name;
            this.template = load(path);
            this.templateGroup = templateGroup;
        }

        @Override
        public TemplateGroup templateGroup() {
            return templateGroup;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum Mouth implements Named {
        NORMAL("Normal", "common/mouth/TypeA"),
        WIDE("Wide", "common/mouth/TypeB"),
        SMILE("Smile", "common/mouth/TypeC"),
        CLEVER("Clever", "common/mouth/TypeD");

        private final String name;
        private final String template;

        Mouth(String name, String path) {
            this.name = name;
            this.template = load(path);
        }

        @Override
        public String getName() {
            return name;
        }

    }

    private interface Named {
        String getName();
    }

    private interface TemplateAligned {
        TemplateGroup templateGroup();
    }

    private static class Binding {
        private final String name;
        private final Function<String, String> source;

        public Binding(String name, String value) {
            this(name, ignore -> value);
        }

        public Binding(String name, Function<String, String> source) {
            this.name = name;
            this.source = source;
        }
    }
}
