package dev.dheirav.thirsttrap.domain

/**
 * The hand-written catalogue. Common houseplants, written properly.
 *
 * The depletion triggers follow docs/WATERING-MODEL.md §2: succulents and cacti
 * 0.7-0.8, most foliage 0.5, moisture-lovers 0.3. They are the single most
 * useful field here, because it is the one number a user has no way to guess.
 *
 * These beat [bundledSpeciesCatalogue] on any query they both match: only these
 * say what actually kills the plant. See [speciesCatalogue] for the union.
 */
val curatedSpeciesCatalogue: List<SpeciesCare> = listOf(

    SpeciesCare(
        name = "Peperomia",
        botanical = "Peperomia spp.",
        aliases = listOf("peperomia", "radiator plant", "peperomia obtusifolia", "baby rubber plant"),
        light = "Bright indirect. Tolerates less, but grows leggy and pale in real shade.",
        water = "Let the top half of the pot dry out. Thick leaves store water, so it is far more forgiving of drought than of a wet pot.",
        medium = Medium.SOIL,
        depletionTrigger = 0.65,
        humidity = "Ordinary room air is fine.",
        toxicity = "Non-toxic to cats and dogs.",
        commonProblems = listOf(
            "Soft, translucent, dropping leaves means too much water - the commonest way these die.",
            "Long bare stems with leaves only at the tips means not enough light.",
        ),
        note = "Semi-succulent. Treat it more like a jade plant than a fern.",
    ),

    SpeciesCare(
        name = "Creeping fig",
        botanical = "Ficus pumila",
        aliases = listOf("creeping fig", "ficus pumila", "climbing fig", "creeping ficus"),
        light = "Bright indirect. Direct midday sun scorches the small leaves.",
        water = "Keep lightly moist - this one is genuinely unforgiving of drying out completely. Water when the top 2 cm feel dry.",
        medium = Medium.SOIL,
        depletionTrigger = 0.35,
        humidity = "Likes it humid. Thrives in a terrarium, sulks in dry winter air.",
        toxicity = "Mildly toxic if eaten; the sap irritates skin.",
        commonProblems = listOf(
            "Crisp brown leaves that drop suddenly usually mean it dried out once, briefly - they rarely come back on that stem.",
            "Leaf drop after being moved is normal; leave it be and it recovers.",
        ),
        note = "One of the few figs that suits a terrarium, being small-leaved and humidity-loving.",
    ),

    SpeciesCare(
        name = "Fiddle leaf fig",
        botanical = "Ficus lyrata",
        aliases = listOf("fiddle leaf fig", "ficus lyrata", "fiddle leaf", "fiddleleaf"),
        light = "As bright as you can manage, short of harsh direct sun.",
        water = "Let the top third dry, then water thoroughly until it runs out the bottom. Hates sitting wet.",
        medium = Medium.SOIL,
        depletionTrigger = 0.5,
        toxicity = "Toxic to cats and dogs.",
        commonProblems = listOf(
            "Brown spots spreading from the leaf edge inward: usually overwatering or root rot.",
            "Brown crispy edges: usually underwatering or dry air.",
            "Dropping leaves after any move is normal. Do not respond by watering more.",
        ),
        note = "Notoriously dramatic about being moved. Pick a spot and leave it.",
    ),

    SpeciesCare(
        name = "Rubber plant",
        botanical = "Ficus elastica",
        aliases = listOf("rubber plant", "ficus elastica", "rubber tree", "rubber fig"),
        light = "Bright indirect. Tolerates medium light with slower growth.",
        water = "Let the top third dry out. Less in winter.",
        medium = Medium.SOIL,
        depletionTrigger = 0.55,
        toxicity = "Toxic to cats and dogs; sap irritates skin.",
        commonProblems = listOf("Lower leaves yellowing and dropping is almost always overwatering."),
    ),

    SpeciesCare(
        name = "Pothos",
        botanical = "Epipremnum aureum",
        aliases = listOf("pothos", "epipremnum", "devils ivy", "money plant", "marble queen", "golden pothos"),
        light = "Anything from low to bright indirect. Variegation fades in low light.",
        water = "Let the top half dry. It wilts visibly when thirsty and recovers within hours, which makes it very easy to read.",
        medium = Medium.SOIL,
        depletionTrigger = 0.6,
        toxicity = "Toxic to cats and dogs.",
        commonProblems = listOf(
            "Yellow leaves usually mean too much water, not too little.",
            "Roots in water indefinitely is fine, but it never transitions to soil as easily afterwards.",
        ),
        note = "The most forgiving houseplant there is, and the easiest to propagate.",
    ),

    SpeciesCare(
        name = "Monstera",
        botanical = "Monstera deliciosa",
        aliases = listOf("monstera", "monstera deliciosa", "swiss cheese plant", "split leaf philodendron"),
        light = "Bright indirect. Direct sun burns; deep shade means no splits in the leaves.",
        water = "Let the top third to half dry out.",
        medium = Medium.SOIL,
        depletionTrigger = 0.5,
        humidity = "Higher humidity gives bigger, better-split leaves.",
        toxicity = "Toxic to cats and dogs.",
        commonProblems = listOf(
            "Leaves without splits usually means not enough light, not immaturity.",
            "Yellowing lower leaves: overwatering.",
            "Weeping droplets from leaf tips is normal - it means it is well watered.",
        ),
    ),

    SpeciesCare(
        name = "Snake plant",
        botanical = "Dracaena trifasciata",
        aliases = listOf("snake plant", "sansevieria", "dracaena trifasciata", "mother in laws tongue"),
        light = "Anything. Genuinely thrives in low light and in a bright window.",
        water = "Let it dry out almost completely. In winter, once a month is often enough.",
        medium = Medium.SOIL,
        depletionTrigger = 0.8,
        toxicity = "Toxic to cats and dogs.",
        commonProblems = listOf(
            "Soft, mushy, falling-over leaves is rot from overwatering, and is usually fatal.",
            "Almost nothing else goes wrong with them.",
        ),
        note = "The plant most often killed by kindness. When unsure, do not water.",
    ),

    SpeciesCare(
        name = "ZZ plant",
        botanical = "Zamioculcas zamiifolia",
        aliases = listOf("zz plant", "zamioculcas", "zanzibar gem", "zz"),
        light = "Low to bright indirect. Extremely tolerant.",
        water = "Let it dry out completely. It stores water in underground rhizomes.",
        medium = Medium.SOIL,
        depletionTrigger = 0.8,
        toxicity = "Toxic to cats and dogs.",
        commonProblems = listOf("Yellowing stems means overwatering. It is very hard to underwater one."),
    ),

    SpeciesCare(
        name = "Philodendron",
        botanical = "Philodendron spp.",
        aliases = listOf("philodendron", "heartleaf philodendron", "philodendron hederaceum", "brasil"),
        light = "Medium to bright indirect.",
        water = "Let the top half dry out.",
        medium = Medium.SOIL,
        depletionTrigger = 0.55,
        toxicity = "Toxic to cats and dogs.",
        commonProblems = listOf("Small leaves and long gaps between them means it wants more light."),
    ),

    SpeciesCare(
        name = "Spider plant",
        botanical = "Chlorophytum comosum",
        aliases = listOf("spider plant", "chlorophytum", "airplane plant"),
        light = "Bright indirect. Tolerates less.",
        water = "Keep lightly moist in growth, drier in winter.",
        medium = Medium.SOIL,
        depletionTrigger = 0.45,
        toxicity = "Non-toxic to cats and dogs.",
        commonProblems = listOf(
            "Brown tips are usually fluoride or chloride in tap water - rainwater or filtered water fixes it.",
            "No babies usually means it needs more light, or a longer day.",
        ),
    ),

    SpeciesCare(
        name = "Maidenhair fern",
        botanical = "Adiantum spp.",
        aliases = listOf("maidenhair fern", "adiantum", "maidenhair"),
        light = "Bright indirect or dappled shade. No direct sun.",
        water = "Never let it dry out, not even once. This is the whole difficulty with them.",
        medium = Medium.SOIL,
        depletionTrigger = 0.25,
        humidity = "High. A bathroom or terrarium suits them far better than a living room.",
        toxicity = "Non-toxic.",
        commonProblems = listOf(
            "Whole fronds crisping brown means it dried out - cut them off at the base, and the plant often regrows.",
            "They are dramatic rather than dead. Keep watering a crisped one for a few weeks.",
        ),
    ),

    SpeciesCare(
        name = "Boston fern",
        botanical = "Nephrolepis exaltata",
        aliases = listOf("boston fern", "nephrolepis", "sword fern"),
        light = "Bright indirect, no direct sun.",
        water = "Keep evenly moist. Dislikes drying out.",
        medium = Medium.SOIL,
        depletionTrigger = 0.3,
        humidity = "High.",
        toxicity = "Non-toxic.",
        commonProblems = listOf("Dropping leaflets everywhere usually means the air is too dry."),
    ),

    SpeciesCare(
        name = "Calathea",
        botanical = "Calathea / Goeppertia spp.",
        aliases = listOf("calathea", "goeppertia", "prayer plant", "maranta", "rattlesnake plant"),
        light = "Medium indirect. Direct sun fades the markings.",
        water = "Keep lightly moist. Sensitive to tap water minerals.",
        medium = Medium.SOIL,
        depletionTrigger = 0.3,
        humidity = "High. The usual reason they look bad indoors.",
        toxicity = "Non-toxic to cats and dogs.",
        commonProblems = listOf(
            "Crispy brown edges: dry air or tap water. Try filtered or rainwater.",
            "Leaves folding up at night is normal and a good sign.",
        ),
        note = "Beautiful and genuinely demanding. Not a beginner plant, whatever the label says.",
    ),

    SpeciesCare(
        name = "Aloe vera",
        botanical = "Aloe barbadensis",
        aliases = listOf("aloe", "aloe vera", "aloe barbadensis"),
        light = "Bright, several hours of direct sun if possible.",
        water = "Soak thoroughly, then let it dry out completely. Perhaps every three weeks.",
        medium = Medium.SOIL,
        depletionTrigger = 0.8,
        toxicity = "Mildly toxic to cats and dogs if eaten.",
        commonProblems = listOf(
            "Flat, spreading, pale leaves means not enough light.",
            "Soft mushy base is rot. Almost always fatal by the time it shows.",
        ),
        note = "Wants gritty, free-draining soil. Ordinary potting compost holds far too much water.",
    ),

    SpeciesCare(
        name = "Jade plant",
        botanical = "Crassula ovata",
        aliases = listOf("jade plant", "crassula", "crassula ovata", "lucky plant", "jade"),
        light = "Bright, direct sun welcome.",
        water = "Let it dry out completely between waterings.",
        medium = Medium.SOIL,
        depletionTrigger = 0.8,
        toxicity = "Toxic to cats and dogs.",
        commonProblems = listOf("Wrinkled leaves means thirsty; soft yellow leaves means overwatered."),
    ),

    SpeciesCare(
        name = "Succulents",
        botanical = "Echeveria, Sedum, Haworthia and similar",
        aliases = listOf("succulent", "succulents", "echeveria", "sedum", "haworthia", "sempervivum"),
        light = "As bright as possible. Most need direct sun to keep their shape and colour.",
        water = "Soak thoroughly, then leave until bone dry. Weeks, not days.",
        medium = Medium.SOIL,
        depletionTrigger = 0.8,
        commonProblems = listOf(
            "Stretching upward with widening gaps between leaves means far too little light.",
            "Translucent, mushy lower leaves means overwatering.",
        ),
        note = "Gritty free-draining mix, and a pot with a drainage hole. Both matter more than watering technique.",
    ),

    SpeciesCare(
        name = "Cactus",
        botanical = "Cactaceae",
        aliases = listOf("cactus", "cacti", "desert cactus"),
        light = "Bright, and most want direct sun.",
        water = "Almost none in winter. Thoroughly but rarely in summer.",
        medium = Medium.SOIL,
        depletionTrigger = 0.85,
        commonProblems = listOf("Soft brown patches at the base is rot, and it moves upward."),
        note = "Christmas and Easter cactus are not this plant - see their own entry. Treating them as desert cacti is the usual way they get killed.",
    ),

    SpeciesCare(
        name = "Christmas cactus",
        botanical = "Schlumbergera spp.",
        aliases = listOf(
            "christmas cactus", "schlumbergera", "thanksgiving cactus", "easter cactus",
            "zygocactus", "holiday cactus",
        ),
        light = "Bright indirect. Direct sun scorches the segments - it grows in tree forks in Brazilian forest, not in a desert.",
        water = "Let the top half dry, then water properly. It wants far more water than a desert cactus, and drops segments when kept as dry as one.",
        medium = Medium.SOIL,
        // Nothing like the 0.85 the desert cacti get. This entry exists because
        // the generated tier disagreed with the old shared "Cactus" entry, and
        // the generated tier was right: an epiphyte is not a succulent.
        depletionTrigger = 0.5,
        humidity = "Appreciates humidity above dry room air.",
        toxicity = "Non-toxic to cats and dogs.",
        commonProblems = listOf(
            "Shrivelled, limp segments usually mean it has been kept too dry, not too wet.",
            "Buds dropping just before flowering is almost always a move, a draught, or a change in light. Leave it alone once buds set.",
        ),
        note = "Needs cool nights and long dark evenings in autumn to set buds. A lamp on in the room at night is the usual reason one never flowers.",
    ),

    SpeciesCare(
        name = "Orchid",
        botanical = "Phalaenopsis spp.",
        aliases = listOf("orchid", "phalaenopsis", "moth orchid"),
        light = "Bright indirect. An east window is close to ideal.",
        water = "Soak the bark weekly, then let it drain completely. Never leave it standing in water.",
        medium = Medium.SEMI_HYDRO,
        depletionTrigger = 0.7,
        humidity = "Moderate to high.",
        toxicity = "Non-toxic.",
        commonProblems = listOf(
            "Limp wrinkled leaves usually means root loss from overwatering, not thirst.",
            "Silvery roots are thirsty; green roots are wet. They tell you directly.",
        ),
        note = "Grows in bark, not soil. Weight readings work well for these, since bark dries predictably.",
    ),

    SpeciesCare(
        name = "Anthurium",
        botanical = "Anthurium andraeanum",
        aliases = listOf("anthurium", "flamingo flower", "laceleaf"),
        light = "Bright indirect.",
        water = "Let the top third dry. Likes airy, chunky soil.",
        medium = Medium.SOIL,
        depletionTrigger = 0.45,
        humidity = "High.",
        toxicity = "Toxic to cats and dogs.",
    ),

    SpeciesCare(
        name = "Alocasia",
        botanical = "Alocasia spp.",
        aliases = listOf("alocasia", "elephant ear", "african mask plant", "polly"),
        light = "Bright indirect.",
        water = "Keep lightly moist but never soggy. Very prone to root rot.",
        medium = Medium.SOIL,
        depletionTrigger = 0.4,
        humidity = "High.",
        toxicity = "Toxic to cats and dogs.",
        commonProblems = listOf(
            "Dropping every leaf and going dormant in winter is normal - the corm is often still alive.",
            "Spider mites love these. Check leaf undersides.",
        ),
    ),

    SpeciesCare(
        name = "Hoya",
        botanical = "Hoya spp.",
        aliases = listOf("hoya", "wax plant", "porcelain flower"),
        light = "Bright indirect, some direct sun encourages flowering.",
        water = "Let it dry out substantially. Thick leaves store water.",
        medium = Medium.SOIL,
        depletionTrigger = 0.7,
        toxicity = "Non-toxic.",
        note = "Do not cut off old flower spurs - it reflowers from the same ones.",
    ),

    SpeciesCare(
        name = "Tradescantia",
        botanical = "Tradescantia zebrina / fluminensis",
        aliases = listOf("tradescantia", "wandering dude", "zebrina", "inch plant", "spiderwort"),
        light = "Bright indirect keeps the colour; low light turns it green and leggy.",
        water = "Let the top half dry.",
        medium = Medium.SOIL,
        depletionTrigger = 0.55,
        toxicity = "Mildly toxic; sap can irritate skin.",
        note = "Roots from a cutting in water within about a week. Almost impossible to fail.",
    ),

    SpeciesCare(
        name = "Syngonium",
        botanical = "Syngonium podophyllum",
        aliases = listOf("syngonium", "arrowhead plant", "arrowhead vine", "goosefoot"),
        light = "Medium to bright indirect.",
        water = "Let the top half dry.",
        medium = Medium.SOIL,
        depletionTrigger = 0.55,
        toxicity = "Toxic to cats and dogs.",
    ),

    SpeciesCare(
        name = "Begonia",
        botanical = "Begonia spp.",
        aliases = listOf("begonia", "rex begonia", "polka dot begonia", "maculata"),
        light = "Bright indirect, no direct sun.",
        water = "Lightly moist. Water the soil, not the leaves - wet foliage invites mildew.",
        medium = Medium.SOIL,
        depletionTrigger = 0.4,
        humidity = "High, but without water sitting on the leaves.",
        toxicity = "Toxic to cats and dogs.",
        commonProblems = listOf("White powdery patches on leaves is mildew, from damp still air."),
    ),

    SpeciesCare(
        name = "Dracaena",
        botanical = "Dracaena spp.",
        aliases = listOf("dracaena", "dragon tree", "marginata", "corn plant", "lucky bamboo"),
        light = "Medium to bright indirect.",
        water = "Let the top half dry. Sensitive to fluoride in tap water.",
        medium = Medium.SOIL,
        depletionTrigger = 0.6,
        toxicity = "Toxic to cats and dogs.",
        commonProblems = listOf("Brown tips with a yellow halo is usually tap water. Try filtered."),
    ),

    SpeciesCare(
        name = "Basil",
        botanical = "Ocimum basilicum",
        aliases = listOf("basil", "ocimum", "tulsi", "holy basil"),
        light = "As much direct sun as you can give it. Six hours is ideal.",
        water = "Keep consistently moist. It wilts dramatically and recovers if caught quickly.",
        medium = Medium.SOIL,
        depletionTrigger = 0.35,
        toxicity = "Edible.",
        commonProblems = listOf(
            "Flowering makes the leaves bitter - pinch the flower buds out.",
            "Leggy stems means too little light.",
        ),
    ),

    SpeciesCare(
        name = "Mint",
        botanical = "Mentha spp.",
        aliases = listOf("mint", "mentha", "peppermint", "spearmint", "pudina"),
        light = "Bright, some direct sun. Tolerates partial shade.",
        water = "Keep moist. Mint is thirstier than most herbs.",
        medium = Medium.SOIL,
        depletionTrigger = 0.3,
        toxicity = "Edible.",
        note = "Keep it in its own pot. In a shared one it takes over completely.",
    ),

    SpeciesCare(
        name = "Chilli",
        botanical = "Capsicum spp.",
        aliases = listOf("chilli", "chili", "chile", "capsicum", "pepper plant", "birds eye chilli"),
        light = "Full sun, as much as possible.",
        water = "Let the top third dry, then water thoroughly. Erratic watering splits the fruit.",
        medium = Medium.SOIL,
        depletionTrigger = 0.5,
        toxicity = "Edible, obviously.",
        commonProblems = listOf(
            "Flowers dropping without fruit usually means no pollination indoors - shake the plant, or use a brush.",
        ),
    ),

    SpeciesCare(
        name = "Money plant (Pilea)",
        botanical = "Pilea peperomioides",
        aliases = listOf("pilea", "pilea peperomioides", "chinese money plant", "pancake plant", "ufo plant"),
        light = "Bright indirect.",
        water = "Let the top half dry. Leaves droop noticeably when thirsty.",
        medium = Medium.SOIL,
        depletionTrigger = 0.55,
        toxicity = "Non-toxic.",
        note = "Rotate it regularly or it leans hard toward the light.",
    ),

    SpeciesCare(
        name = "String of pearls",
        botanical = "Curio rowleyanus",
        aliases = listOf("string of pearls", "senecio rowleyanus", "curio rowleyanus", "string of beads"),
        light = "Very bright, some direct sun.",
        water = "Let it dry out completely. Shrivelled pearls means thirsty; mushy ones mean overwatered.",
        medium = Medium.SOIL,
        depletionTrigger = 0.8,
        toxicity = "Toxic to cats and dogs.",
        note = "Shallow-rooted. A wide shallow pot suits it far better than a deep one.",
    ),

    SpeciesCare(
        name = "Air plant",
        botanical = "Tillandsia spp.",
        aliases = listOf("air plant", "tillandsia", "airplant"),
        light = "Bright indirect.",
        water = "Soak in water for 20 minutes weekly, then shake out and dry upside down. Trapped water at the base rots them.",
        medium = Medium.UNKNOWN,
        depletionTrigger = 0.8,
        toxicity = "Non-toxic.",
        note = "No soil at all, so weight tracking does not apply here.",
    ),


    // --- terrarium plants ---
    // Added after the first pass skipped them entirely, which was an odd gap
    // in an app whose author keeps a terrarium.

    SpeciesCare(
        name = "Fittonia",
        botanical = "Fittonia albivenis",
        aliases = listOf("fittonia", "nerve plant", "mosaic plant", "fittonia albivenis"),
        light = "Medium to bright indirect. Direct sun scorches the thin leaves within hours.",
        water = "Keep consistently damp - never wet, never dry. It faints dramatically when thirsty and usually recovers within an hour of watering.",
        medium = Medium.SOIL,
        depletionTrigger = 0.25,
        humidity = "High. This is why it is a terrarium staple and a windowsill disappointment.",
        toxicity = "Non-toxic to cats and dogs.",
        commonProblems = listOf(
            "Collapsing flat is thirst, not death - water it and wait an hour before doing anything else.",
            "Crisp brown edges mean the air is too dry rather than the soil.",
            "Leggy stems with bare gaps: pinch the tips out to keep it bushy.",
        ),
        note = "One of the few plants that genuinely tells you when it is thirsty. In a closed terrarium it barely needs watering at all.",
    ),

    SpeciesCare(
        name = "Polka dot plant",
        botanical = "Hypoestes phyllostachya",
        aliases = listOf("polka dot plant", "hypoestes", "freckle face"),
        light = "Bright indirect. Colour fades badly in low light.",
        water = "Keep lightly moist.",
        medium = Medium.SOIL,
        depletionTrigger = 0.3,
        humidity = "High. Another terrarium regular.",
        toxicity = "Non-toxic.",
        commonProblems = listOf("Flowering means it is about to get leggy and decline - pinch the spikes off."),
    ),

    SpeciesCare(
        name = "Baby tears",
        botanical = "Soleirolia soleirolii",
        aliases = listOf("baby tears", "soleirolia", "angel tears", "mind your own business"),
        light = "Medium indirect.",
        water = "Constantly damp. It browns off within a day of drying out.",
        medium = Medium.SOIL,
        depletionTrigger = 0.2,
        humidity = "Very high. Effectively a terrarium-only plant indoors.",
        toxicity = "Non-toxic.",
        note = "Spreads into a dense mat and makes good ground cover under taller terrarium plants.",
    ),

    SpeciesCare(
        name = "Club moss",
        botanical = "Selaginella spp.",
        aliases = listOf("club moss", "selaginella", "spikemoss", "resurrection plant"),
        light = "Low to medium indirect. Never direct.",
        water = "Constantly damp.",
        medium = Medium.SOIL,
        depletionTrigger = 0.2,
        humidity = "Very high - it will not survive open room air for long.",
        toxicity = "Non-toxic.",
    ),

    // --- common houseplants the first pass missed ---

    SpeciesCare(
        name = "Peace lily",
        botanical = "Spathiphyllum spp.",
        aliases = listOf("peace lily", "spathiphyllum"),
        light = "Low to medium indirect. One of the genuinely low-light-tolerant flowering plants.",
        water = "Let the top third dry. It wilts theatrically when thirsty and recovers within hours - but repeated wilting shortens its life.",
        medium = Medium.SOIL,
        depletionTrigger = 0.4,
        toxicity = "Toxic to cats and dogs.",
        commonProblems = listOf(
            "No flowers usually means not enough light, despite its low-light reputation.",
            "Brown tips are usually tap water minerals.",
        ),
    ),

    SpeciesCare(
        name = "Chinese evergreen",
        botanical = "Aglaonema spp.",
        aliases = listOf("chinese evergreen", "aglaonema"),
        light = "Low to medium indirect. Darker varieties tolerate less light than variegated ones.",
        water = "Let the top half dry.",
        medium = Medium.SOIL,
        depletionTrigger = 0.55,
        toxicity = "Toxic to cats and dogs.",
        note = "Among the most tolerant of neglect and low light of anything with interesting foliage.",
    ),

    SpeciesCare(
        name = "Croton",
        botanical = "Codiaeum variegatum",
        aliases = listOf("croton", "codiaeum", "josephs coat"),
        light = "Very bright, some direct sun. Colour is entirely light-dependent.",
        water = "Keep lightly moist.",
        medium = Medium.SOIL,
        depletionTrigger = 0.4,
        humidity = "Moderate to high.",
        toxicity = "Toxic to cats and dogs; sap irritates skin.",
        commonProblems = listOf(
            "Dropping every leaf after being moved or repotted is normal for these, and it usually regrows.",
            "Green new growth on a colourful variety means too little light.",
        ),
    ),

    SpeciesCare(
        name = "Ivy",
        botanical = "Hedera helix",
        aliases = listOf("ivy", "english ivy", "hedera", "hedera helix"),
        light = "Bright indirect. Variegated forms need more.",
        water = "Let the top half dry.",
        medium = Medium.SOIL,
        depletionTrigger = 0.5,
        toxicity = "Toxic to cats and dogs.",
        commonProblems = listOf("Spider mites are almost inevitable indoors in dry air. Check the undersides regularly."),
    ),

    SpeciesCare(
        name = "Kalanchoe",
        botanical = "Kalanchoe blossfeldiana",
        aliases = listOf("kalanchoe", "flaming katy"),
        light = "Bright, direct sun welcome.",
        water = "Let it dry out well between waterings.",
        medium = Medium.SOIL,
        depletionTrigger = 0.75,
        toxicity = "Toxic to cats and dogs.",
        note = "Reflowering needs genuinely long dark nights for several weeks - about fourteen hours of uninterrupted darkness.",
    ),

    SpeciesCare(
        name = "String of hearts",
        botanical = "Ceropegia woodii",
        aliases = listOf("string of hearts", "ceropegia", "rosary vine", "chain of hearts"),
        light = "Bright, including some direct sun.",
        water = "Let it dry out fully. It stores water in tubers along the vine.",
        medium = Medium.SOIL,
        depletionTrigger = 0.75,
        toxicity = "Non-toxic.",
        note = "The little tubers along the strands root readily - lay one on soil and it becomes a new plant.",
    ),

    SpeciesCare(
        name = "Asparagus fern",
        botanical = "Asparagus setaceus / densiflorus",
        aliases = listOf("asparagus fern", "asparagus setaceus", "foxtail fern", "sprengeri"),
        light = "Bright indirect.",
        water = "Keep lightly moist. Tuberous roots make it more drought-tolerant than a true fern.",
        medium = Medium.SOIL,
        depletionTrigger = 0.45,
        toxicity = "Toxic to cats and dogs; the berries especially.",
        note = "Not actually a fern. It has thorns, which is a surprise the first time.",
    ),

    SpeciesCare(
        name = "Oxalis",
        botanical = "Oxalis triangularis",
        aliases = listOf("oxalis", "purple shamrock", "false shamrock", "wood sorrel"),
        light = "Bright indirect to some direct sun.",
        water = "Let the top half dry.",
        medium = Medium.SOIL,
        depletionTrigger = 0.5,
        toxicity = "Toxic to cats and dogs in quantity.",
        commonProblems = listOf(
            "Dying back completely is dormancy, not death - stop watering, wait a few weeks, and it returns from the bulbs.",
        ),
        note = "Folds its leaves down at night and opens them in the morning.",
    ),

    SpeciesCare(
        name = "Coleus",
        botanical = "Coleus scutellarioides",
        aliases = listOf("coleus", "painted nettle", "solenostemon"),
        light = "Bright indirect. Some direct sun deepens the colour, too much bleaches it.",
        water = "Keep lightly moist. Wilts fast and recovers fast.",
        medium = Medium.SOIL,
        depletionTrigger = 0.35,
        toxicity = "Mildly toxic to pets.",
        note = "Pinch out flower spikes and the growing tips, or it goes leggy and stops making leaves.",
    ),

    SpeciesCare(
        name = "Aralia",
        botanical = "Polyscias spp.",
        aliases = listOf("aralia", "polyscias", "ming aralia"),
        light = "Bright indirect.",
        water = "Let the top half dry.",
        medium = Medium.SOIL,
        depletionTrigger = 0.5,
        humidity = "Moderate to high.",
        toxicity = "Toxic to cats and dogs.",
        commonProblems = listOf("Sudden leaf drop after a move or a draught is characteristic. Keep it steady and wait."),
    ),

    SpeciesCare(
        name = "Areca palm",
        botanical = "Dypsis lutescens",
        aliases = listOf("areca palm", "dypsis", "butterfly palm", "golden cane palm"),
        light = "Bright indirect.",
        water = "Keep lightly moist. Sensitive to fluoride and salts in tap water.",
        medium = Medium.SOIL,
        depletionTrigger = 0.4,
        toxicity = "Non-toxic to cats and dogs.",
        commonProblems = listOf(
            "Brown tips are usually tap water or dry air.",
            "Spider mites thrive on these indoors.",
        ),
    ),

    SpeciesCare(
        name = "Money tree",
        botanical = "Pachira aquatica",
        aliases = listOf("money tree", "pachira", "pachira aquatica", "guiana chestnut"),
        light = "Bright indirect.",
        water = "Let the top half dry. Despite growing in swamps in the wild, it rots readily in a pot.",
        medium = Medium.SOIL,
        depletionTrigger = 0.55,
        toxicity = "Non-toxic.",
    ),

    SpeciesCare(
        name = "Moss",
        botanical = "Bryophyta",
        aliases = listOf("moss", "sheet moss", "cushion moss"),
        light = "Low to medium indirect. Direct sun bleaches and kills it.",
        water = "Keep constantly damp. Mist rather than pour.",
        medium = Medium.SPHAGNUM,
        depletionTrigger = 0.2,
        humidity = "Very high - which is why it belongs in a closed terrarium.",
        toxicity = "Non-toxic.",
        commonProblems = listOf("Going brown and crisp means it dried; going black and slimy means no air movement."),
    ),
)

/**
 * Everything the app can look up.
 *
 * The hand-written entries come first, each widened with any old botanical
 * synonyms and stray common names the generated tier turned up for the same
 * plant, then the generated long tail. Order is not what decides ties -
 * [findSpeciesCare] ranks curated above bundled explicitly - but keeping it
 * stable makes the tests readable.
 */
val speciesCatalogue: List<SpeciesCare> =
    curatedSpeciesCatalogue.map { entry ->
        val extra = bundledAliasesForCurated[entry.name].orEmpty()
        if (extra.isEmpty()) entry else entry.copy(aliases = entry.aliases + extra)
    } + bundledSpeciesCatalogue
