class QrVerifyResult {
  final bool valide;
  final String resultat;
  final String message;
  final String? codeCarte;
  final String? nomAffiche;
  final String? iun;
  final String? paroisseOrigine;
  final String? dateBapteme;
  final bool? etatLibreMatrimonial;
  final List<String> mentionsInscrites;
  final String? remplaceeParCodeCarte;

  const QrVerifyResult({
    required this.valide,
    required this.resultat,
    required this.message,
    this.codeCarte,
    this.nomAffiche,
    this.iun,
    this.paroisseOrigine,
    this.dateBapteme,
    this.etatLibreMatrimonial,
    required this.mentionsInscrites,
    this.remplaceeParCodeCarte,
  });

  factory QrVerifyResult.fromJson(Map<String, dynamic> j) => QrVerifyResult(
        valide: j['valide'] as bool? ?? false,
        resultat: j['resultat'] as String? ?? 'INVALIDE_OU_FALSIFIE',
        message: j['message'] as String? ?? '',
        codeCarte: j['codeCarte'] as String?,
        nomAffiche: j['nomAffiche'] as String?,
        iun: j['iun'] as String?,
        paroisseOrigine: j['paroisseOrigine'] as String?,
        dateBapteme: j['dateBapteme'] as String?,
        etatLibreMatrimonial: j['etatLibreMatrimonial'] as bool?,
        mentionsInscrites: ((j['mentionsInscrites'] as List?) ?? []).map((e) => e.toString()).toList(),
        remplaceeParCodeCarte: j['remplaceeParCodeCarte'] as String?,
      );
}
