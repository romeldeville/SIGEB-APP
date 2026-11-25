import 'package:flutter_riverpod/flutter_riverpod.dart';

final localeProvider = StateProvider<String>((ref) => 'FR');

class AppStrings {
  final String lang;
  const AppStrings(this.lang);

  bool get isEn => lang == 'EN';

  String get appTitle => 'SIGEB Cameroun';
  String get appSubtitle => isEn
      ? 'National Baptism Register — Catholic Church in Cameroon'
      : 'Registre National des Baptêmes — Église Catholique du Cameroun';

  String get login => isEn ? 'Sign In' : 'Connexion';
  String get logout => isEn ? 'Sign Out' : 'Déconnexion';
  String get identifier => isEn ? 'Email or phone' : 'Email ou téléphone';
  String get password => isEn ? 'Password' : 'Mot de passe';
  String get totpCode => isEn ? '2FA Code (if enabled)' : 'Code 2FA (si activé)';
  String get scanPublicQr => isEn ? 'Verify a Baptism Card (QR)' : 'Vérifier une carte de baptême (QR)';

  String get navDashboard => isEn ? 'Dashboard' : 'Tableau de bord';
  String get navActes => isEn ? 'Baptisms' : 'Actes';
  String get navScan => isEn ? 'Verify QR' : 'Vérifier QR';
  String get navMentions => isEn ? 'Notations' : 'Mentions';
  String get navReferentiel => isEn ? 'Directory' : 'Référentiel';
  String get navAudit => isEn ? 'Audit' : 'Audit';

  String get sealedActs => isEn ? 'Sealed Acts' : 'Actes scellés';
  String get pendingActs => isEn ? 'Pending Validation' : 'En attente Curé';
  String get drafts => isEn ? 'Drafts / To Fix' : 'Brouillons / À corriger';
  String get originalCards => isEn ? 'Original Cards' : 'Cartes originales';
  String get duplicates => isEn ? 'Duplicates' : 'Duplicatas';

  String get newAct => isEn ? 'New Baptism Act' : 'Nouvel acte de baptême';
  String get sealAct => isEn ? 'Seal Act (PIN)' : 'Sceller l\'acte (PIN)';
  String get issueCard => isEn ? 'Issue Card / Duplicate' : 'Émettre carte / duplicata';
  String get printPdf => isEn ? 'Print / Preview PDF' : 'Imprimer / Aperçu PDF';
  String get nationalSearch => isEn ? 'National Search' : 'Recherche nationale';
  String get verifyIntegrity => isEn ? 'Verify Audit Chain' : 'Vérifier chaîne SHA-256';
}

final stringsProvider = Provider<AppStrings>((ref) {
  return AppStrings(ref.watch(localeProvider));
});
