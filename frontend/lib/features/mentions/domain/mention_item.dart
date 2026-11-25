class MentionItem {
  final String id;
  final String iun;
  final String paroisseDeclaranteNomFr;
  final String typeSacrement;
  final String dateEvenement;
  final String lieuEvenement;
  final String ministreCelebrant;
  final String? conjointNomComplet;
  final String statut;

  const MentionItem({
    required this.id,
    required this.iun,
    required this.paroisseDeclaranteNomFr,
    required this.typeSacrement,
    required this.dateEvenement,
    required this.lieuEvenement,
    required this.ministreCelebrant,
    this.conjointNomComplet,
    required this.statut,
  });

  factory MentionItem.fromJson(Map<String, dynamic> j) => MentionItem(
        id: j['id'] as String,
        iun: j['iun'] as String,
        paroisseDeclaranteNomFr: j['paroisseDeclaranteNomFr'] as String? ?? '',
        typeSacrement: j['typeSacrement'] as String,
        dateEvenement: j['dateEvenement'] as String,
        lieuEvenement: j['lieuEvenement'] as String,
        ministreCelebrant: j['ministreCelebrant'] as String,
        conjointNomComplet: j['conjointNomComplet'] as String?,
        statut: j['statut'] as String,
      );
}
