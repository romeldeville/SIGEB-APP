import 'package:flutter/material.dart';

class ScreenBreakpoints {
  static const double mobile = 600;
  static const double tablet = 1024;
  static const double desktop = 1280;

  static bool isMobile(BuildContext context) => MediaQuery.sizeOf(context).width < mobile;
  static bool isTablet(BuildContext context) {
    final w = MediaQuery.sizeOf(context).width;
    return w >= mobile && w < tablet;
  }

  static bool isDesktop(BuildContext context) => MediaQuery.sizeOf(context).width >= tablet;

  static double horizontalPadding(double width) {
    if (width < mobile) return 14;
    if (width < tablet) return 22;
    return 32;
  }
}

/// Centre le contenu sur grands écrans tout en gardant un padding adaptatif sur mobile.
class ResponsiveContainer extends StatelessWidget {
  final Widget child;
  final double maxWidth;

  const ResponsiveContainer({
    super.key,
    required this.child,
    this.maxWidth = 1200,
  });

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final pad = ScreenBreakpoints.horizontalPadding(constraints.maxWidth);
        return Align(
          alignment: Alignment.topCenter,
          child: ConstrainedBox(
            constraints: BoxConstraints(maxWidth: maxWidth),
            child: Padding(
              padding: EdgeInsets.symmetric(horizontal: pad, vertical: 16),
              child: child,
            ),
          ),
        );
      },
    );
  }
}

/// Grille fluide calculée dynamiquement selon la largeur réelle disponible (aucune largeur fixe cassée).
class ResponsiveGrid extends StatelessWidget {
  final List<Widget> children;
  final double minItemWidth;
  final double spacing;
  final int maxColumns;

  const ResponsiveGrid({
    super.key,
    required this.children,
    this.minItemWidth = 170,
    this.spacing = 12,
    this.maxColumns = 5,
  });

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final available = constraints.maxWidth;
        final cols = (available / (minItemWidth + spacing)).floor().clamp(1, maxColumns);
        final itemWidth = (available - spacing * (cols - 1)) / cols;

        return Wrap(
          spacing: spacing,
          runSpacing: spacing,
          children: children
              .map((child) => SizedBox(width: itemWidth, child: child))
              .toList(),
        );
      },
    );
  }
}
