class AuthSession {
  final String accessToken;
  final String refreshToken;
  final String role;
  final String niveau;
  final String nomComplet;
  final String? provinceId;
  final String? dioceseId;
  final String? paroisseId;
  final bool mdpAChanger;
  final bool totpActive;

  const AuthSession({
    required this.accessToken,
    required this.refreshToken,
    required this.role,
    required this.niveau,
    required this.nomComplet,
    this.provinceId,
    this.dioceseId,
    this.paroisseId,
    required this.mdpAChanger,
    required this.totpActive,
  });

  factory AuthSession.fromJson(Map<String, dynamic> json) {
    return AuthSession(
      accessToken: json['accessToken'] as String,
      refreshToken: json['refreshToken'] as String,
      role: json['role'] as String,
      niveau: json['niveau'] as String,
      nomComplet: json['nomComplet'] as String,
      provinceId: json['provinceId'] as String?,
      dioceseId: json['dioceseId'] as String?,
      paroisseId: json['paroisseId'] as String?,
      mdpAChanger: json['mdpAChanger'] as bool? ?? false,
      totpActive: json['totpActive'] as bool? ?? false,
    );
  }

  AuthSession copyWith({
    String? nomComplet,
    bool? mdpAChanger,
    bool? totpActive,
  }) {
    return AuthSession(
      accessToken: accessToken,
      refreshToken: refreshToken,
      role: role,
      niveau: niveau,
      nomComplet: nomComplet ?? this.nomComplet,
      provinceId: provinceId,
      dioceseId: dioceseId,
      paroisseId: paroisseId,
      mdpAChanger: mdpAChanger ?? this.mdpAChanger,
      totpActive: totpActive ?? this.totpActive,
    );
  }

  bool get isCure => role == 'CURE';
  bool get isChancelier => role == 'CHANCELIER';
  bool get isResponsableProvince => role == 'RESPONSABLE_PROVINCE';
  bool get isAdminNational => role == 'ADMIN_NATIONAL';
  bool get isParoisse => niveau == 'PAROISSE';
  bool get canSignActs =>
      role == 'CURE' || role == 'CHANCELIER' || role == 'RESPONSABLE_PROVINCE' || role == 'ADMIN_NATIONAL';
  bool get canRevokeCard => role == 'CURE' || role == 'CHANCELIER' || role == 'ADMIN_NATIONAL';
  bool get canMutateUsers => role == 'ADMIN_NATIONAL' || role == 'RESPONSABLE_PROVINCE' || role == 'CHANCELIER';
}
