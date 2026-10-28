package com.example.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.AppLanguage
import com.example.data.model.UserRole

/**
 * Centralized Arabic (RTL), French (LTR), and English (LTR) copy for the Multi-Role Account,
 * Progressive Resumable Onboarding, Visitor Mode, Role Switcher, and Personalized Dashboards.
 */
object RoleStrings {

  fun tr(language: AppLanguage, ar: String, fr: String, en: String): String =
    when (language) {
      AppLanguage.AR -> ar
      AppLanguage.FR -> fr
      AppLanguage.EN -> en
    }

  fun iconForRole(role: UserRole): ImageVector =
    when (role) {
      UserRole.FARMER -> Icons.Filled.Agriculture
      UserRole.TRADER -> Icons.Filled.Storefront
      UserRole.TRANSPORTER -> Icons.Filled.LocalShipping
      UserRole.SUPPLIER -> Icons.Filled.Inventory2
      UserRole.SERVICE_PROVIDER -> Icons.Filled.Handyman
      UserRole.STORAGE_PROVIDER -> Icons.Filled.AcUnit
    }

  fun onboardingWelcomeTitle(language: AppLanguage): String =
    tr(
      language,
      ar = "مرحبًا بك في فلاح نوميريك. كيف تريد استخدام المنصة؟",
      fr = "Bienvenue sur Fellah Numérique. Comment souhaitez-vous utiliser la plateforme ?",
      en = "Welcome to Fellah Numérique. How would you like to use the platform?",
    )

  fun onboardingWelcomeSubtitle(language: AppLanguage): String =
    tr(
      language,
      ar = "يمكنك اختيار نشاط مهني واحد أو عدة أنشطة معًا، وتبديل نشاطك الفعال أو إضافة أنشطة أخرى لاحقًا من إعدادات حسابك دون الحاجة لإنشاء حساب جديد.",
      fr = "Vous pouvez sélectionner une ou plusieurs activités professionnelles, basculer entre vos rôles à tout moment et les modifier plus tard dans vos paramètres.",
      en = "You can select one or several professional activities now, switch active roles anytime without logging out, and add or remove roles later.",
    )

  fun visitorExploreCta(language: AppLanguage): String =
    tr(
      language,
      ar = "تصفح المنصة كزائر بدون تسجيل",
      fr = "Explorer la plateforme en visiteur",
      en = "Browse Platform as Visitor",
    )

  fun visitorBannerTitle(language: AppLanguage): String =
    tr(
      language,
      ar = "وضع الزائر العام • تصفح حر بدون حساب",
      fr = "Mode Visiteur Public • Navigation libre",
      en = "Public Visitor Mode • Browse without registration",
    )

  fun visitorSignInGateMessage(language: AppLanguage): String =
    tr(
      language,
      ar = "لإتمام هذا الإجراء (الشراء، النشر، طلب عرض سعر، أو مراسلة المتعاملين)، يرجى تسجيل الدخول بحساب Google وسنوجهك مباشرة إلى وجهتك.",
      fr = "Pour effectuer cette action (acheter, publier, demander un devis ou contacter un opérateur), connectez-vous avec Google. Votre destination sera conservée.",
      en = "To complete this action (buy, publish, request quotes, or message operators), sign in with Google and we will take you straight to your destination.",
    )

  fun activeRoleLabel(language: AppLanguage): String =
    tr(language, ar = "النشاط الحالي:", fr = "Rôle actif :", en = "Active role:")

  fun switchRoleMenuTitle(language: AppLanguage): String =
    tr(
      language,
      ar = "التبديل السريع بين أنشطتك المهنية",
      fr = "Basculer entre vos activités professionnelles",
      en = "Switch Active Professional Role",
    )

  fun addAnotherRoleLabel(language: AppLanguage): String =
    tr(
      language,
      ar = "+ إضافة نشاط مهني جديد",
      fr = "+ Ajouter une activité professionnelle",
      en = "+ Add Professional Activity",
    )

  fun incompleteProfileBannerTitle(language: AppLanguage, role: UserRole): String =
    tr(
      language,
      ar = "أكمل بياناتك المهنية كـ (${role.titleAr}) لزيادة موثوقية عروضك",
      fr = "Complétez votre profil (${role.titleFr}) pour renforcer votre visibilité",
      en = "Complete your (${role.titleEn}) profile to boost marketplace trust",
    )

  fun completeProfileCta(language: AppLanguage): String =
    tr(language, ar = "إكمال الملف الآن", fr = "Compléter maintenant", en = "Complete Profile")

  fun completeLaterCta(language: AppLanguage): String =
    tr(language, ar = "الإكمال لاحقًا والدخول للمنصة", fr = "Compléter plus tard", en = "Complete Later")

  fun catalogSectionHeadline(language: AppLanguage, role: UserRole): String =
    when (role) {
      UserRole.TRANSPORTER ->
        tr(
          language,
          ar = "أسطول النقل وخطوط الشحن المنشورة",
          fr = "Ma Flotte & Services de Transport",
          en = "My Fleet & Published Transport Routes",
        )
      UserRole.SUPPLIER ->
        tr(
          language,
          ar = "كتالوج البذور والأسمدة ومعدات الري",
          fr = "Catalogue Semences, Engrais & Irrigation",
          en = "Seeds, Fertilizers & Irrigation Catalog",
        )
      UserRole.SERVICE_PROVIDER ->
        tr(
          language,
          ar = "خدماتي الزراعية والهندسية المنشورة",
          fr = "Mes Prestations & Services Agricoles",
          en = "Published Agricultural & Engineering Services",
        )
      UserRole.STORAGE_PROVIDER ->
        tr(
          language,
          ar = "غرف التبريد والمستودعات والسعات المتاحة",
          fr = "Entrepôts, Chambres Froides & Capacités",
          en = "Warehouses, Cold Rooms & Available Capacity",
        )
      else ->
        tr(
          language,
          ar = "كتالوج عروض الشركاء والممونين",
          fr = "Catalogue des Partenaires Agricoles",
          en = "Agricultural Partner Catalog",
        )
    }

  fun orderStatusLabel(status: String, language: AppLanguage): String =
    when (status) {
      "pending" -> tr(language, "قيد الانتظار", "En attente", "Pending")
      "quoted" -> tr(language, "تم تقديم عرض سعر", "Devis proposé", "Quoted")
      "accepted" -> tr(language, "مقبول وقيد التنفيذ", "Accepté / En cours", "Accepted")
      "completed" -> tr(language, "مكتمل", "Terminé", "Completed")
      "cancelled" -> tr(language, "ملغى", "Annulé", "Cancelled")
      else -> status
    }
}
