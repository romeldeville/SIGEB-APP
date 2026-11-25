class ActeBapteme {
  final String id;
  final String? iun;
  final String paroisseId;
  final String paroisseNomFr;
  final String origine;
  final String statut;
  final int version;
  final String nom;
  final String prenoms;
  final String? nomBapteme;
  final String sexe;
  final String dateNaissance;
  final String lieuNaissance;
  final String? nomPere;
  final String? nomMere;
  final String? nomParrain;
  final String? nomMarraine;
  final String dateBapteme;
  final String lieuBapteme;
  final String ministreCelebrant;
  final String registreTome;
  final String registrePage;
  final String registreNumero;
  final String? motifCorrection;

  const ActeBapteme({
    required this.id,
    this.iun,
    required this.paroisseId,
    required this.paroisseNomFr,
    required this.origine,
    required this.statut,
    required this.version,
    required this.nom,
    required this.prenoms,
    this.nomBapteme,
    required this.sexe,
    required this.dateNaissance,
    required this.lieuNaissance,
    this.nomPere,
    this.nomMere,
    this.nomParrain,
    this.nomMarraine,
    required this.dateBapteme,
    required this.lieuBapteme,
    required this.ministreCelebrant,
    required this.registreTome,
    required this.registrePage,
    required this.registreNumero,
    this.motifCorrection,
  });

  factory ActeBapteme.fromJson(Map<String, dynamic> j) => ActeBapteme(
        id: j['id'] as String,
        iun: j['iun'] as String?,
        paroisseId: j['paroisseId'] as String,
        paroisseNomFr: j['paroisseNomFr'] as String? ?? '',
        origine: j['origine'] as String,
        statut: j['statut'] as String,
        version: (j['version'] as num?)?.toInt() ?? 1,
        nom: j['nom'] as String,
        prenoms: j['prenoms'] as String,
        nomBapteme: j['nomBapteme'] as String?,
        sexe: j['sexe'] as String,
        dateNaissance: j['dateNaissance'] as String,
        lieuNaissance: j['lieuNaissance'] as String,
        nomPere: j['nomPere'] as String?,
        nomMere: j['nomMere'] as String?,
        nomParrain: j['nomParrain'] as String?,
        nomMarraine: j['nomMarraine'] as String?,
        dateBapteme: j['dateBapteme'] as String,
        lieuBapteme: j['lieuBapteme'] as String,
        ministreCelebrant: j['ministreCelebrant'] as String,
        registreTome: j['registreTome'] as String,
        registrePage: j['registrePage'] as String,
        registreNumero: j['registreNumero'] as String,
        motifCorrection: j['motifCorrection'] as String?,
      );

  bool get isScelle => statut == 'SCELLE';
}

class CarteEmise {
  final String id;
  final String codeCarte;
  final String formatDocument;
  final String langue;
  final String typeEmission;
  final int numeroDuplicata;
  final String statut;
  final String qrPayload;

  const CarteEmise({
    required this.id,
    required this.codeCarte,
    required this.formatDocument,
    required this.langue,
    required this.typeEmission,
    required this.numeroDuplicata,
    required this.statut,
    required this.qrPayload,
  });

  factory CarteEmise.fromJson(Map<String, dynamic> j) => CarteEmise(
        id: j['id'] as String,
        codeCarte: j['codeCarte'] as String,
        formatDocument: j['formatDocument'] as String,
        langue: j['langue'] as String,
        typeEmission: j['typeEmission'] as String,
        numeroDuplicata: (j['numeroDuplicata'] as num).toInt(),
        statut: j['statut'] as String,
        qrPayload: j['qrPayload'] as String,
      );

  bool get isActive => statut == 'ACTIVE';
}

class AvenantRectification {
  final String id;
  final String acteBaptemeId;
  final int versionCible;
  final String gravite;
  final Map<String, dynamic> nouvellesValeurs;
  final String motif;
  final String? referenceJustificatif;
  final String statut;
  final String? demandeLe;
  final String? valideLe;

  const AvenantRectification({
    required this.id,
    required this.acteBaptemeId,
    required this.versionCible,
    required this.gravite,
    required this.nouvellesValeurs,
    required this.motif,
    this.referenceJustificatif,
    required this.statut,
    this.demandeLe,
    this.valideLe,
  });

  factory AvenantRectification.fromJson(Map<String, dynamic> j) => AvenantRectification(
        id: j['id'] as String,
        acteBaptemeId: j['acteBaptemeId'] as String,
        versionCible: (j['versionCible'] as num).toInt(),
        gravite: j['gravite'] as String,
        nouvellesValeurs: Map<String, dynamic>.from(j['nouvellesValeurs'] as Map? ?? {}),
        motif: j['motif'] as String,
        referenceJustificatif: j['referenceJustificatif'] as String?,
        statut: j['statut'] as String,
        demandeLe: j['demandeLe'] as String?,
        valideLe: j['valideLe'] as String?,
      );

  bool get isEnAttente => statut == 'EN_ATTENTE_CHANCELIER';
}
