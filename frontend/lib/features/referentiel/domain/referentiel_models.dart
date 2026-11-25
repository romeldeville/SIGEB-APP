class ProvinceItem {
  final String id;
  final String code;
  final String nomFr;
  final String nomEn;

  const ProvinceItem({required this.id, required this.code, required this.nomFr, required this.nomEn});

  factory ProvinceItem.fromJson(Map<String, dynamic> j) => ProvinceItem(
        id: j['id'] as String,
        code: j['code'] as String,
        nomFr: j['nomFr'] as String,
        nomEn: j['nomEn'] as String,
      );
}

class DioceseItem {
  final String id;
  final String provinceId;
  final String provinceCode;
  final String code;
  final String typeDiocese;
  final String nomFr;
  final String nomEn;
  final String siege;

  const DioceseItem({
    required this.id,
    required this.provinceId,
    required this.provinceCode,
    required this.code,
    required this.typeDiocese,
    required this.nomFr,
    required this.nomEn,
    required this.siege,
  });

  factory DioceseItem.fromJson(Map<String, dynamic> j) => DioceseItem(
        id: j['id'] as String,
        provinceId: j['provinceId'] as String,
        provinceCode: j['provinceCode'] as String,
        code: j['code'] as String,
        typeDiocese: j['typeDiocese'] as String,
        nomFr: j['nomFr'] as String,
        nomEn: j['nomEn'] as String,
        siege: j['siege'] as String,
      );
}

class ParoisseItem {
  final String id;
  final String dioceseId;
  final String dioceseCode;
  final String code;
  final String typeParoisse;
  final String nomFr;
  final String nomEn;
  final String localite;

  const ParoisseItem({
    required this.id,
    required this.dioceseId,
    required this.dioceseCode,
    required this.code,
    required this.typeParoisse,
    required this.nomFr,
    required this.nomEn,
    required this.localite,
  });

  factory ParoisseItem.fromJson(Map<String, dynamic> j) => ParoisseItem(
        id: j['id'] as String,
        dioceseId: j['dioceseId'] as String,
        dioceseCode: j['dioceseCode'] as String,
        code: j['code'] as String,
        typeParoisse: j['typeParoisse'] as String,
        nomFr: j['nomFr'] as String,
        nomEn: j['nomEn'] as String,
        localite: j['localite'] as String,
      );
}

class UtilisateurItem {
  final String id;
  final String identifiant;
  final String nomComplet;
  final String role;
  final String niveau;
  final String statut;
  final bool totpActive;

  const UtilisateurItem({
    required this.id,
    required this.identifiant,
    required this.nomComplet,
    required this.role,
    required this.niveau,
    required this.statut,
    required this.totpActive,
  });

  factory UtilisateurItem.fromJson(Map<String, dynamic> j) => UtilisateurItem(
        id: j['id'] as String,
        identifiant: j['identifiant'] as String,
        nomComplet: j['nomComplet'] as String,
        role: j['role'] as String,
        niveau: j['niveau'] as String,
        statut: j['statut'] as String,
        totpActive: j['totpActive'] as bool? ?? false,
      );
}
