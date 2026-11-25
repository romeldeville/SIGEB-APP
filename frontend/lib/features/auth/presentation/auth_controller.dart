import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/storage/token_storage.dart';
import '../data/auth_repository.dart';
import '../domain/auth_session.dart';

class AuthState {
  final AuthSession? session;
  final bool loading;
  final String? error;

  const AuthState({this.session, this.loading = false, this.error});

  bool get isAuthenticated => session != null;

  AuthState copyWith({AuthSession? session, bool? loading, String? error, bool clearSession = false}) {
    return AuthState(
      session: clearSession ? null : (session ?? this.session),
      loading: loading ?? this.loading,
      error: error,
    );
  }
}

class AuthController extends StateNotifier<AuthState> {
  final AuthRepository _repo;
  final TokenStorage _storage;

  AuthController(this._repo, this._storage) : super(const AuthState());

  Future<bool> login(String identifiant, String motDePasse, {String? totpCode}) async {
    state = state.copyWith(loading: true, error: null);
    try {
      final session = await _repo.login(
        identifiant: identifiant,
        motDePasse: motDePasse,
        totpCode: totpCode,
      );
      await _storage.saveTokens(
        accessToken: session.accessToken,
        refreshToken: session.refreshToken,
      );
      state = AuthState(session: session, loading: false);
      return true;
    } catch (e) {
      state = state.copyWith(loading: false, error: extractApiError(e));
      return false;
    }
  }

  Future<void> logout() async {
    await _storage.clear();
    state = const AuthState();
  }
}

final authControllerProvider = StateNotifierProvider<AuthController, AuthState>((ref) {
  return AuthController(
    ref.watch(authRepositoryProvider),
    ref.watch(tokenStorageProvider),
  );
});
