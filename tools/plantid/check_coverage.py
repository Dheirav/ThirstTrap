"""Does this label set know houseplants exist?

The question is not accuracy, it is coverage: a class that is absent cannot be
predicted however good the model is.
"""
import csv, collections

rows = list(csv.DictReader(open('model/labelmap.csv')))
names = [r['name'] for r in rows if r['name'] != 'background']
genera = collections.Counter(n.split()[0] for n in names)

# Common indoor genera, from the app's own curated catalogue plus the usual
# garden-centre shelf.
indoor = """Monstera Epipremnum Philodendron Sansevieria Dracaena Spathiphyllum
Calathea Goeppertia Aglaonema Zamioculcas Hoya Tradescantia Chlorophytum
Anthurium Syngonium Alocasia Colocasia Dieffenbachia Schefflera Begonia Pilea
Crassula Echeveria Haworthia Aloe Kalanchoe Sedum Peperomia Fittonia Maranta
Ctenanthe Stromanthe Nephrolepis Asplenium Adiantum Platycerium Davallia
Ficus Citrus Musa Strelitzia Yucca Beaucarnea Cordyline Hedera Scindapsus
Rhaphidophora Oxalis Saintpaulia Streptocarpus Gasteria Senecio Ceropegia
Dischidia Aeschynanthus Columnea Cissus Soleirolia Selaginella""".split()

present = [g for g in indoor if genera.get(g, 0) > 0]
absent = [g for g in indoor if genera.get(g, 0) == 0]

print(f"label set: {len(names)} classes, {len(genera)} genera")
print(f"indoor genera checked: {len(indoor)}")
print(f"  present: {len(present)}  ({100*len(present)/len(indoor):.0f}%)")
print(f"  absent : {len(absent)}   ({100*len(absent)/len(indoor):.0f}%)")
print()
print("present:", ", ".join(present))
print()
print("absent :", ", ".join(absent))
print()
print("the twenty largest genera in the label set, for a sense of what it is for:")
for g, c in genera.most_common(20):
    print(f"  {c:3d}  {g}")
