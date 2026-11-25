class StatistiqueGlobale {
  final String niveau;
  final int totalActesScelles;
  final int totalActesEnAttente;
  final int totalBrouillonsOuACorriger;
  final int totalCartesOriginales;
  final int totalDuplicatas;
  final Map<String, int> mentionsParType;
  final Map<String, int> baptemesParAnnee;

  const StatistiqueGlobale({
    required this.niveau,
    required this.totalActesScelles,
    required this.totalActesEnAttente,
    required this.totalBrouillonsOuACorriger,
    required this.totalCartesOriginales,
    required this.totalDuplicatas,
    required this.mentionsParType,
    required this.baptemesParAnnee,
  });

  factory StatistiqueGlobale.fromJson(Map<String, dynamic> json) {
    return StatistiqueGlobale(
      niveau: json['niveau'] as String? ?? '',
      totalActesScelles: (json['totalActesScelles'] as num?)?.toInt() ?? 0,
      totalActesEnAttente: (json['totalActesEnAttente'] as num?)?.toInt() ?? 0,
      totalBrouillonsOuACorriger: (json['totalBrouillonsOuACorriger'] as num?)?.toInt() ?? 0,
      totalCartesOriginales: (json['totalCartesOriginales'] as num?)?.toInt() ?? 0,
      totalDuplicatas: (json['totalDuplicatas'] as num?)?.toInt() ?? 0,
      mentionsParType: ((json['mentionsParType'] as Map?) ?? {})
          .map((k, v) => MapEntry(k.toString(), (v as num).toInt())),
      baptemesParAnnee: ((json['baptemesParAnnee'] as Map?) ?? {})
          .map((k, v) => MapEntry(k.toString(), (v as num).toInt())),
    );
  }
}
